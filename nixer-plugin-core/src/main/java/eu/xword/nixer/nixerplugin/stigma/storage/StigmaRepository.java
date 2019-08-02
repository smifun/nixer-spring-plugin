package eu.xword.nixer.nixerplugin.stigma.storage;

import eu.xword.nixer.nixerplugin.login.LoginResult;
import eu.xword.nixer.nixerplugin.stigma.StigmaToken;

public interface StigmaRepository {
    void save(final StigmaToken stigma, final LoginResult loginResult);
}
