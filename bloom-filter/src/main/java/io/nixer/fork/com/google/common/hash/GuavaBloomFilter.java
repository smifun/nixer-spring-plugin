/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.nixer.fork.com.google.common.hash;

import java.io.Serializable;
import java.math.RoundingMode;
import java.nio.ByteOrder;
import javax.annotation.Nonnull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.hash.Funnel;
import com.google.common.math.DoubleMath;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.nixer.bloom.BitArray;
import io.nixer.bloom.BitArray.Factory;
import io.nixer.bloom.BloomFilterParameters;
import io.nixer.bloom.BloomFilter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Copied from Guava 22.0 com.google.common.hash.BloomFilterStrategies - all changes marked with "CROSSWORD CHANGES"
 * <br>
 * See: https://github.com/google/guava/
 * <br>
 * License: http://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * <p>
 * A Bloom filter for instances of {@code T}. A Bloom filter offers an approximate containment test
 * with one-sided error: if it claims that an element is contained in it, this might be in error,
 * but if it claims that an element is <i>not</i> contained in it, then this is definitely true.
 *
 * <p>If you are unfamiliar with Bloom filters, this nice <a
 * href="http://llimllib.github.com/bloomfilter-tutorial/">tutorial</a> may help you understand how
 * they work.
 *
 * <p>The false positive probability ({@code FPP}) of a Bloom filter is defined as the probability
 * that {@linkplain #mightContain(Object)} will erroneously return {@code true} for an object that
 * has not actually been put in the {@code GuavaBloomFilter}.
 *
 *  // CROSSWORD CHANGES - removed no longer valid comment about serialisation
 *
 * @param <T> the type of instances that the {@code GuavaBloomFilter} accepts
 * @author Dimitris Andreou
 * @author Kevin Bourrillion
 * @since 11.0
 */
// CROSSWORD CHANGES - added interface io.nixer.bloom.BloomFilter and removed Serializable
public final class GuavaBloomFilter<T> implements BloomFilter<T> {
  /**
   * A strategy to translate T instances, to {@code numHashFunctions} bit indexes.
   *
   * <p>Implementations should be collections of pure functions (i.e. stateless).
   */

// CROSSWORD CHANGES - made public
  public interface Strategy extends Serializable {

    /**
     * Sets {@code numHashFunctions} bits of the given bit array, by hashing a user element.
     *
     * <p>Returns whether any bits changed as a result of this operation.
     */
    <T> boolean put(T object, Funnel<? super T> funnel, int numHashFunctions, BitArray bits);

    /**
     * Queries {@code numHashFunctions} bits of the given bit array, by hashing a user element;
     * returns {@code true} if and only if all selected bits are set.
     */
    <T> boolean mightContain(
            T object, Funnel<? super T> funnel, int numHashFunctions, BitArray bits);

    /**
     * Identifier used to encode this strategy, when marshalled as part of a GuavaBloomFilter. Only
     * values in the [-128, 127] range are valid for the compact serial form. Non-negative values
     * are reserved for enums defined in GuavaBloomFilterStrategies; negative values are reserved for any
     * custom, stateful strategy we may define (e.g. any kind of strategy that would depend on user
     * input).
     */
    int ordinal();
  }

  /** The bit set of the GuavaBloomFilter (not necessarily power of 2!) */
  private final BitArray bits;

  /** Number of hashes per element */
  private final int numHashFunctions;

  /** The funnel to translate Ts to bytes */
  private final Funnel<? super T> funnel;

  /**
   * The strategy we employ to map an element T to {@code numHashFunctions} bit indexes.
   */
  private final Strategy strategy;


  // CROSSWORD CHANGES - added field parameters
  private final BloomFilterParameters parameters;

  /**
   * Creates a GuavaBloomFilter.
   */
  // CROSSWORD CHANGES - constructor made package private and added "parameters" argument
  GuavaBloomFilter(
          BitArray bits, int numHashFunctions, Funnel<? super T> funnel, Strategy strategy, @Nonnull final BloomFilterParameters parameters) {
    checkArgument(numHashFunctions > 0, "numHashFunctions (%s) must be > 0", numHashFunctions);
    checkArgument(
        numHashFunctions <= 255, "numHashFunctions (%s) must be <= 255", numHashFunctions);
    this.bits = checkNotNull(bits);
    this.numHashFunctions = numHashFunctions;
    this.funnel = checkNotNull(funnel);
    this.strategy = checkNotNull(strategy);
    this.parameters = parameters;
  }

  //  --- CROSSWORD CHANGES - removed copy() method

  /**
   * Returns {@code true} if the element <i>might</i> have been put in this Bloom filter,
   * {@code false} if this is <i>definitely</i> not the case.
   */
  @Override
  public boolean mightContain(T object) {
    return strategy.mightContain(object, funnel, numHashFunctions, bits);
  }

  /**
   * @deprecated Provided only to satisfy the {@link Predicate} interface; use {@link #mightContain}
   *     instead.
   */
  @Deprecated
  @Override
  //  --- CROSSWORD CHANGES - @SuppressFBWarnings
  public boolean apply(T input) {
    return mightContain(input);
  }

  /**
   * Puts an element into this {@code GuavaBloomFilter}. Ensures that subsequent invocations of {@link
   * #mightContain(Object)} with the same element will always return {@code true}.
   *
   * @return true if the Bloom filter's bits changed as a result of this operation. If the bits
   *     changed, this is <i>definitely</i> the first time {@code object} has been added to the
   *     filter. If the bits haven't changed, this <i>might</i> be the first time {@code object} has
   *     been added to the filter. Note that {@code put(t)} always returns the <i>opposite</i>
   *     result to what {@code mightContain(t)} would have returned at the time it is called.
   * @since 12.0 (present in 11.0 with {@code void} return type})
   */
  @Override
  @CanIgnoreReturnValue
  public boolean put(T object) {
    return strategy.put(object, funnel, numHashFunctions, bits);
  }

  /**
   * Returns the probability that {@linkplain #mightContain(Object)} will erroneously return
   * {@code true} for an object that has not actually been put in the {@code GuavaBloomFilter}.
   *
   * <p>Ideally, this number should be close to the {@code fpp} parameter passed in
   * {@linkplain #create(Funnel, int, double, Factory, BloomFilterParameters)}, or smaller. If it is significantly higher, it is
   * usually the case that too many elements (more than expected) have been put in the
   * {@code GuavaBloomFilter}, degenerating it.
   *
   * @since 14.0 (since 11.0 as expectedFalsePositiveProbability())
   */
  @Override
  public double expectedFpp() {
    // You down with FPP? (Yeah you know me!) Who's down with FPP? (Every last homie!)
    return Math.pow((double) bits.bitCount() / bitSize(), numHashFunctions);
  }

  /**
   * Returns an estimate for the total number of distinct elements that have been added to this
   * Bloom filter. This approximation is reasonably accurate if it does not exceed the value of
   * {@code expectedInsertions} that was used when constructing the filter.
   *
   * @since 22.0
   */
  @Override
  public long approximateElementCount() {
    long bitSize = bits.bitSize();
    long bitCount = bits.bitCount();

    /**
     * Each insertion is expected to reduce the # of clear bits by a factor of
     * `numHashFunctions/bitSize`. So, after n insertions, expected bitCount is `bitSize * (1 - (1 -
     * numHashFunctions/bitSize)^n)`. Solving that for n, and approximating `ln x` as `x - 1` when x
     * is close to 1 (why?), gives the following formula.
     */
    double fractionOfBitsSet = (double) bitCount / bitSize;
    return DoubleMath.roundToLong(
        -Math.log1p(-fractionOfBitsSet) * bitSize / numHashFunctions, RoundingMode.HALF_UP);
  }

  @Nonnull
  @Override
  public BloomFilterParameters getParameters() {
    return this.parameters;
  }

  /**
   * Returns the number of bits in the underlying bit array.
   */
  @VisibleForTesting
  long bitSize() {
    return bits.bitSize();
  }

  /**
   * Determines whether a given Bloom filter is compatible with this Bloom filter. For two Bloom
   * filters to be compatible, they must:
   *
   * <ul>
   *   <li>not be the same instance
   *   <li>have the same number of hash functions
   *   <li>have the same bit size
   *   <li>have the same strategy
   *   <li>have equal funnels
   * </ul>
   *
   * @param that The Bloom filter to check for compatibility.
   * @since 15.0
   */
  public boolean isCompatible(GuavaBloomFilter<T> that) {
    checkNotNull(that);
    return (this != that)
        && (this.numHashFunctions == that.numHashFunctions)
        && (this.bitSize() == that.bitSize())
        && (this.strategy.equals(that.strategy))
        && (this.funnel.equals(that.funnel));
  }


  //  --- CROSSWORD CHANGES - removed putAll() method

  //  --- CROSSWORD CHANGES - removed hashCode() and equals()

  /**
   * Creates a {@link GuavaBloomFilter}{@code <T>} with the expected number of insertions and
   * expected false positive probability.
   *
   * <p>Note that overflowing a {@code GuavaBloomFilter} with significantly more elements than specified,
   * will result in its saturation, and a sharp deterioration of its false positive probability.
   *
   * <p>The constructed {@code GuavaBloomFilter<T>} will be serializable if the provided
   * {@code Funnel<T>} is.
   *
   * <p>It is recommended that the funnel be implemented as a Java enum. This has the benefit of
   * ensuring proper serialization and deserialization, which is important since {@link #equals}
   * also relies on object identity of funnels.
   *
   * @param funnel the funnel of T's that the constructed {@code GuavaBloomFilter<T>} will use
   * @param expectedInsertions the number of expected insertions to the constructed
   *     {@code GuavaBloomFilter<T>}; must be positive
   * @param fpp the desired false positive probability (must be positive and less than 1.0)
   * @param bitArrayFactory a factory used to create new bit arrays
   * @param parameters
   * @return a {@code GuavaBloomFilter}
   * @since 19.0
   */
  // CROSSWORD CHANGES - added bitArrayFactory argument
  public static <T> GuavaBloomFilter<T> create(Funnel<? super T> funnel, long expectedInsertions, double fpp, final Factory bitArrayFactory) {
    return create(funnel, expectedInsertions, fpp, GuavaBloomFilterStrategies.MURMUR128_MITZ_64, bitArrayFactory);
  }

  // CROSSWORD CHANGES - added new variation of create() that accepts BloomFilterParameters
  public static <T> GuavaBloomFilter<T> create(Funnel<? super T> funnel, final Factory bitArrayFactory, final BloomFilterParameters parameters) {
    try {
      return new GuavaBloomFilter<T>(
              bitArrayFactory.create(parameters.getBitSize()),
              parameters.getNumHashFunctions(),
              funnel,
              parameters.getStrategy(),
              parameters
      );
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Could not create GuavaBloomFilter of " + parameters.getNumHashFunctions() + " bits", e);
    }
  }


  @VisibleForTesting
  // CROSSWORD CHANGES - added bitArrayFactory argument
  static <T> GuavaBloomFilter<T> create(
          Funnel<? super T> funnel, long expectedInsertions, double fpp, GuavaBloomFilterStrategies strategy, final Factory bitArrayFactory) {
    checkNotNull(funnel);
    checkArgument(
        expectedInsertions >= 0, "Expected insertions (%s) must be >= 0", expectedInsertions);
    checkArgument(fpp > 0.0, "False positive probability (%s) must be > 0.0", fpp);
    checkArgument(fpp < 1.0, "False positive probability (%s) must be < 1.0", fpp);
    checkNotNull(strategy);

    if (expectedInsertions == 0) {
      expectedInsertions = 1;
    }
    /*
     * TODO(user): Put a warning in the javadoc about tiny fpp values, since the resulting size
     * is proportional to -log(p), but there is not much of a point after all, e.g.
     * optimalM(1000, 0.0000000000000001) = 76680 which is less than 10kb. Who cares!
     */
    long numBits = optimalNumOfBits(expectedInsertions, fpp);
    int numHashFunctions = optimalNumOfHashFunctions(expectedInsertions, numBits);

    // CROSSWORD CHANGES - added creation targetParameters
    final BloomFilterParameters targetParameters = new BloomFilterParameters(expectedInsertions, fpp, numHashFunctions, numBits, strategy, ByteOrder.nativeOrder());

    try {
      // CROSSWORD CHANGES - replaced "new BitArray()" with "bitArrayFactory.create()"
      return new GuavaBloomFilter<T>(bitArrayFactory.create(numBits), numHashFunctions, funnel, strategy, targetParameters);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Could not create GuavaBloomFilter of " + numBits + " bits", e);
    }
  }

  // CROSSWORD CHANGES - removed some overloaded variations of create() method

  // Cheat sheet:
  //
  // m: total bits
  // n: expected insertions
  // b: m/n, bits per insertion
  // p: expected false positive probability
  //
  // 1) Optimal k = b * ln2
  // 2) p = (1 - e ^ (-kn/m))^k
  // 3) For optimal k: p = 2 ^ (-k) ~= 0.6185^b
  // 4) For optimal k: m = -nlnp / ((ln2) ^ 2)

  /**
   * Computes the optimal k (number of hashes per element inserted in Bloom filter), given the
   * expected insertions and total number of bits in the Bloom filter.
   *
   * See http://en.wikipedia.org/wiki/File:Bloom_filter_fp_probability.svg for the formula.
   *
   * @param n expected insertions (must be positive)
   * @param m total number of bits in Bloom filter (must be positive)
   */
  @VisibleForTesting
  static int optimalNumOfHashFunctions(long n, long m) {
    // (m / n) * log(2), but avoid truncation due to division!
    return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
  }

  /**
   * Computes m (total bits of Bloom filter) which is expected to achieve, for the specified
   * expected insertions, the required false positive probability.
   *
   * See http://en.wikipedia.org/wiki/Bloom_filter#Probability_of_false_positives for the formula.
   *
   * @param n expected insertions (must be positive)
   * @param p false positive rate (must be 0 < p < 1)
   */
  @VisibleForTesting
  static long optimalNumOfBits(long n, double p) {
    if (p == 0) {
      p = Double.MIN_VALUE;
    }
    return (long) (-n * Math.log(p) / (Math.log(2) * Math.log(2)));
  }




  // CROSSWORD CHANGES - removed writeReplace(), writeTo() and readFrom() methods and SerialForm class

}
