package io.nixer.nixerplugin.core.stigma.token;


import com.nimbusds.jwt.JWT;

/**
 * Created on 2019-05-20.
 *
 * @author gcwiak
 */
public interface StigmaTokenProvider {

    JWT getToken(String stigmaValue);
}
