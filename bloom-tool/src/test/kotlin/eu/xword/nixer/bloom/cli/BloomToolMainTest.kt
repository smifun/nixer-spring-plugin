package eu.xword.nixer.bloom.cli

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.PrintStream


/**
 * Tests for [BloomToolMain] CLI.
 *
 * Created on 23/08/2018.
 *
 * @author cezary
 */
class BloomToolMainTest {

    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    private val commandOutput = ByteArrayOutputStream()
    private val originalOut = System.out

    @Before
    fun setUp() {
        System.setOut(PrintStream(commandOutput))
    }

    @After
    fun tearDown() {
        System.setOut(originalOut)
    }

    @Test
    fun `should create bloom filter`() {
        // given
        val filterFile = givenFile("test.bloom")

        // when
        executeCommand("create", "--size=100", "--fpp=1e-2", "--name=${filterFile.absolutePath}")

        // then
        assertThat(filterFile).exists()
    }

    @Test
    fun `should insert values into bloom filter and execute successful check`() {
        // given
        val filterFile = givenFile("test.bloom")
        executeCommand("create", "--size=3", "--fpp=1e-2", "--name=${filterFile.absolutePath}")
        assertThat(filterFile).exists()

        val hashToLookFor = "CBFDAC6008F9CAB4083784CBD1874F76618D2A97" // password123 SHA-1

        val hexHashes = listOf(
                "BB928CA332F5F4FA2CDAEF238672E0FBCF5E7A0F", // foobar1 SHA-1
                hashToLookFor,
                "42E1D179E9781138DF3471EEF084F6622A0E7091" // IamTheBest SHA-1
        )

        val valuesFile = givenFile("values.txt").apply {
            printWriter().use { hexHashes.forEach { hash -> it.println(hash) } }
        }

        val checkFile = givenFile("check.txt").apply { writeText(hashToLookFor) }

        // when
        executeCommand("insert", "--input-file=${valuesFile.absolutePath}", "--name=${filterFile.absolutePath}")

        executeCommand("check", "--input-file=${checkFile.absolutePath}", "--name=${filterFile.absolutePath}", "--hashed")

        // then
        assertThat(commandOutput.toString()).contains(hashToLookFor)
    }

    @Test
    fun `should build bloom filter from unparsed entries and execute successful check`() {
        // given
        val filterFile = givenFile("test.bloom")

        val hashToLookFor = "CBFDAC6008F9CAB4083784CBD1874F76618D2A97" // password123 SHA-1

        val hexHashesWithAdditionalColumn = listOf(
                "BB928CA332F5F4FA2CDAEF238672E0FBCF5E7A0F:54", // foobar1 SHA-1
                "$hashToLookFor:7",
                "42E1D179E9781138DF3471EEF084F6622A0E7091:2" // IamTheBest SHA-1
        )

        val valuesFile = givenFile("values.txt").apply {
            printWriter().use { hexHashesWithAdditionalColumn.forEach { hash -> it.println(hash) } }
        }

        val checkFile = givenFile("check.txt").apply { writeText(hashToLookFor) }

        // when
        executeCommand("build",
                "--size=3", "--fpp=1e-2", "--input-file=${valuesFile.absolutePath}", "--separator=:", "--field=0",
                "--name=${filterFile.absolutePath}")

        executeCommand("check", "--input-file=${checkFile.absolutePath}", "--name=${filterFile.absolutePath}", "--hashed")

        // then
        assertThat(commandOutput.toString()).contains(hashToLookFor)
    }

    @Test
    fun `should build bloom filter from not hashed values and execute successful check using hash`() {
        // given
        val filterFile = givenFile("test.bloom")

        val hashToLookFor = "CBFDAC6008F9CAB4083784CBD1874F76618D2A97" // password123 SHA-1

        val notHashedValues = listOf(
                "foobar1",
                "password123",
                "IamTheBest"
        )

        val valuesFile = givenFile("values.txt").apply {
            printWriter().use { notHashedValues.forEach { hash -> it.println(hash) } }
        }

        val checkFile = givenFile("check.txt").apply { writeText(hashToLookFor) }

        // when
        executeCommand("build",
                "--size=3", "--fpp=1e-2", "--input-file=${valuesFile.absolutePath}",
                "--name=${filterFile.absolutePath}",
                "--sha1"
        )

        executeCommand("check", "--input-file=${checkFile.absolutePath}", "--name=${filterFile.absolutePath}", "--hashed")

        // then
        assertThat(commandOutput.toString()).contains(hashToLookFor)
    }

    @Test
    fun `should build bloom filter from not hashed values and execute successful check using raw value`() {
        // given
        val filterFile = givenFile("test.bloom")

        val valueToLookFor = "password123"

        val notHashedValues = listOf(
                "foobar1",
                valueToLookFor,
                "IamTheBest"
        )

        val valuesFile = givenFile("values.txt").apply {
            printWriter().use { notHashedValues.forEach { hash -> it.println(hash) } }
        }

        val checkFile = givenFile("check.txt").apply { writeText(valueToLookFor) }

        // when
        executeCommand("build",
                "--size=3", "--fpp=1e-2", "--input-file=${valuesFile.absolutePath}",
                "--name=${filterFile.absolutePath}",
                "--sha1"
        )

        executeCommand("check", "--input-file=${checkFile.absolutePath}", "--name=${filterFile.absolutePath}")

        // then
        assertThat(commandOutput.toString()).contains(valueToLookFor)
    }

    private fun executeCommand(vararg command: String) {
        main(arrayOf(*command))
    }

    private fun givenFile(name: String): File {
        val file = temporaryFolder.newFile(name)
        delete(file)
        return file
    }

    private fun delete(file: File) {
        if (!file.delete()) {
            throw IOException("Failed to delete: $file")
        }
    }
}