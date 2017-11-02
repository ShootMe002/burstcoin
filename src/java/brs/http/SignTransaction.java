package brs.http;

import brs.BurstException;
import brs.Transaction;
import brs.crypto.Crypto;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static brs.http.JSONResponses.MISSING_SECRET_PHRASE;

public final class SignTransaction extends APIServlet.APIRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(SignTransaction.class);

    static final SignTransaction instance = new SignTransaction();

    private SignTransaction() {
        super(new APITag[] {APITag.TRANSACTIONS}, "unsignedTransactionBytes", "unsignedTransactionJSON", "secretPhrase");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

        String transactionBytes = Convert.emptyToNull(req.getParameter("unsignedTransactionBytes"));
        String transactionJSON = Convert.emptyToNull(req.getParameter("unsignedTransactionJSON"));
        Transaction transaction = ParameterParser.parseTransaction(transactionBytes, transactionJSON);

        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        }

        JSONObject response = new JSONObject();
        try {
            transaction.validate();
            if (transaction.getSignature() != null) {
                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect unsigned transaction - already signed");
                return response;
            }
            if (! Arrays.equals(Crypto.getPublicKey(secretPhrase), transaction.getSenderPublicKey())) {
                response.put("errorCode", 4);
                response.put("errorDescription", "Secret phrase doesn't match transaction sender public key");
                return response;
            }
            transaction.sign(secretPhrase);
            response.put("transaction", transaction.getStringId());
            response.put("fullHash", transaction.getFullHash());
            response.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
            response.put("signatureHash", Convert.toHexString(Crypto.sha256().digest(transaction.getSignature())));
            response.put("verify", transaction.verifySignature() && transaction.verifyPublicKey());
        } catch (BurstException.ValidationException|RuntimeException e) {
            logger.debug(e.getMessage(), e);
            response.put("errorCode", 4);
            response.put("errorDescription", "Incorrect unsigned transaction: " + e.toString());
            response.put("error", e.getMessage());
            return response;
        }
        return response;
    }

}
