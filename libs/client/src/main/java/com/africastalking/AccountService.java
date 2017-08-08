package com.africastalking;


import com.africastalking.interfaces.IAccount;
import com.africastalking.models.AccountResponse;

import com.africastalking.proto.SdkServerServiceOuterClass;
import retrofit2.Response;
import retrofit2.Retrofit;

import java.io.IOException;

/**
 * Account service. Retrieve user account info
 */
public class AccountService extends Service {

    private static AccountService sInstance;
    private IAccount service;

    AccountService() throws IOException {
        super();
    }

    @Override
    protected AccountService getInstance() throws IOException {

        if (sInstance == null) {
            sInstance = new AccountService();
        }

        return sInstance;
    }

    @Override
    protected void fetchToken(String host, int port) throws IOException {
        fetchServiceToken(host, port, SdkServerServiceOuterClass.ClientTokenRequest.Capability.UNRECOGNIZED);
    }

    @Override
    protected void initService() {
        String url = "https://api."+ (AfricasTalking.ENV == Environment.SANDBOX ? Const.SANDBOX_DOMAIN : Const.PRODUCTION_DOMAIN);
        url += "/version1/";
        Retrofit retrofit = retrofitBuilder
                .baseUrl(url)
                .build();

        service = retrofit.create(IAccount.class);
    }

    @Override
    protected boolean isInitialized() {
        return sInstance != null;
    }

    @Override
    protected void destroyService() {
        if (sInstance != null) {
            sInstance = null;
        }
    }


    // ->

    /**
     * Get user info.
     * <p>
     *     Synchronously send the request and return its response.
     * </p>
     * @return String in specified format, xml or json
     * @throws IOException
     */
    public AccountResponse getUser() throws IOException {
        Response<AccountResponse> resp = service.getUser(username).execute();
        return resp.body();
    }

    /**
     * Get user info.
     * <p>
     *     Asynchronously send the request and notify {@code callback} of its response or if an error
     * occurred
     * </p>
     * @param callback
     */
    public void getUser(final Callback<AccountResponse> callback) {
        service.getUser(username).enqueue(makeCallback(callback));
    }
}
