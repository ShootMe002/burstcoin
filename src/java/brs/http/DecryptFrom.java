package brs.http;

import brs.Account;
import brs.BurstException;
import brs.crypto.EncryptedData;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.DECRYPTION_FAILED;
import static brs.http.JSONResponses.INCORRECT_ACCOUNT;

public final class DecryptFrom extends APIServlet.APIRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(DecryptFrom.class);

    static final DecryptFrom instance = new DecryptFrom();

    private DecryptFrom() {
        super(new APITag[] {APITag.MESSAGES}, "account", "data", "nonce", "decryptedMessageIsText", "secretPhrase");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

        Account account = ParameterParser.getAccount(req);
        if (account.getPublicKey() == null) {
            return INCORRECT_ACCOUNT;
        }
        String secretPhrase = ParameterParser.getSecretPhrase(req);
        byte[] data = Convert.parseHexString(Convert.nullToEmpty(req.getParameter("data")));
        byte[] nonce = Convert.parseHexString(Convert.nullToEmpty(req.getParameter("nonce")));
        EncryptedData encryptedData = new EncryptedData(data, nonce);
        boolean isText = !"false".equalsIgnoreCase(req.getParameter("decryptedMessageIsText"));
        try {
            byte[] decrypted = account.decryptFrom(encryptedData, secretPhrase);
            JSONObject response = new JSONObject();
            response.put("decryptedMessage", isText ? Convert.toString(decrypted) : Convert.toHexString(decrypted));
            return response;
        } catch (RuntimeException e) {
            logger.debug(e.toString());
            return DECRYPTION_FAILED;
        }
    }

}
