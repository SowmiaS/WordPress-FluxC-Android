package org.wordpress.android.stores.module;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.network.discovery.SelfHostedEndpointFinder;
import org.wordpress.android.stores.network.rest.wpcom.account.AccountRestClient;
import org.wordpress.android.stores.network.rest.wpcom.auth.AccessToken;
import org.wordpress.android.stores.network.rest.wpcom.auth.Authenticator;
import org.wordpress.android.stores.network.rest.wpcom.post.PostRestClient;
import org.wordpress.android.stores.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.stores.network.xmlrpc.post.PostXMLRPCClient;
import org.wordpress.android.stores.network.xmlrpc.site.SiteXMLRPCClient;
import org.wordpress.android.stores.store.AccountStore;
import org.wordpress.android.stores.store.PostStore;
import org.wordpress.android.stores.store.SiteStore;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ReleaseStoreModule {
    @Provides
    @Singleton
    public SiteStore provideSiteStore(Dispatcher dispatcher, SiteRestClient siteRestClient,
                                      SiteXMLRPCClient siteXMLRPCClient) {
        return new SiteStore(dispatcher, siteRestClient, siteXMLRPCClient);
    }

    @Provides
    @Singleton
    public AccountStore provideUserStore(Dispatcher dispatcher, AccountRestClient accountRestClient,
                                         SelfHostedEndpointFinder selfHostedEndpointFinder, Authenticator authenticator,
                                         AccessToken accessToken) {
        return new AccountStore(dispatcher, accountRestClient, selfHostedEndpointFinder, authenticator, accessToken);
    }

    @Provides
    @Singleton
    public PostStore providePostStore(Dispatcher dispatcher, PostRestClient postRestClient,
                                      PostXMLRPCClient postXMLRPCClient) {
        return new PostStore(dispatcher, postRestClient, postXMLRPCClient);
    }
}
