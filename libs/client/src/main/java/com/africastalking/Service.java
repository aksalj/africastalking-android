package com.africastalking;

import com.africastalking.proto.SdkServerServiceGrpc;
import com.google.gson.GsonBuilder;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import com.africastalking.proto.SdkServerServiceGrpc.*;
import com.africastalking.proto.SdkServerServiceOuterClass.*;
import okhttp3.*;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.*;
import retrofit2.Call;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;


/**
 * A given service offered by AT API
 */
abstract class Service {

    Retrofit.Builder retrofitBuilder;

    ClientTokenResponse token;

    String username;

    Service() throws IOException {

        this.username = AfricasTalking.USERNAME;

        if(token == null || token.getExpiration() < System.currentTimeMillis()) {
            fetchToken(AfricasTalking.HOST, AfricasTalking.PORT);
        }

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

        if (AfricasTalking.LOGGING) {
            HttpLoggingInterceptor logger = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(String message) {
                    AfricasTalking.LOGGER.log(message);
                }
            });
            logger.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpClient.addInterceptor(logger);
        }

        httpClient.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();
                Request request = original.newBuilder()
                        .addHeader("Token", token.getToken())
                        .addHeader("Accept", "application/json")
                        .build();

                return chain.proceed(request);
            }
        });


        retrofitBuilder = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().setLenient().create())) // switched from ScalarsConverterFactory
                .client(httpClient.build());

        initService();
    }

    static ManagedChannel getChannel(String host, int port) {
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext(true); // FIXME: Remove to Setup TLS
        return channelBuilder.build();
    }

    protected ClientTokenResponse fetchServiceToken(String host, int port, ClientTokenRequest.Capability capability) throws IOException {
        ManagedChannel channel = getChannel(host, port);
        SdkServerServiceBlockingStub stub = SdkServerServiceGrpc.newBlockingStub(channel);
        ClientTokenRequest req = ClientTokenRequest.newBuilder()
                .setCapability(capability)
                .setEnvironment(AfricasTalking.ENV.toString())
                .build();
        return stub.getToken(req);
    }


    /**
     *
     * @param cb
     * @param <T>
     * @return
     */
    protected <T> retrofit2.Callback<T> makeCallback(final Callback<T> cb) {
        return new retrofit2.Callback<T>() {
            @Override
            public void onResponse(Call<T> call, retrofit2.Response<T> response) {
                if (response.isSuccessful()) {
                    cb.onSuccess(response.body());
                } else {
                    cb.onFailure(new Exception(response.message()));
                }
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                cb.onFailure(t);
            }
        };
    }


    protected abstract void fetchToken(String host, int port) throws IOException;

    /**
     * Get an instance of a service.
     * @param <T>
     * @return
     */
    protected abstract <T extends Service> T getInstance() throws IOException;

    /**
     * Check if a service is initialized
     * @return boolean true if yes, false otherwise
     */
    protected abstract boolean isInitialized();

    /**
     * Initializes a service
     */
    protected abstract void initService();

    /**
     * Destroys a service
     */
    protected abstract void destroyService();
}
