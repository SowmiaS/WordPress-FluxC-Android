package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.WooCommerceStore.FetchWCProductSettingsResponsePayload;
import org.wordpress.android.fluxc.store.WooCommerceStore.FetchWCSiteSettingsResponsePayload;

@ActionEnum
public enum WCCoreAction implements IAction {
    // Remote actions
    @Action(payloadType = SiteModel.class)
    FETCH_SITE_SETTINGS,
    @Action(payloadType = SiteModel.class)
    FETCH_PRODUCT_SETTINGS,

    // Remote responses
    @Action(payloadType = FetchWCSiteSettingsResponsePayload.class)
    FETCHED_SITE_SETTINGS,
    @Action(payloadType = FetchWCProductSettingsResponsePayload.class)
    FETCHED_PRODUCT_SETTINGS
}
