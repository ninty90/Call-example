package tw.com.chainsea.call.base;

import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCore;

/**
 * OnLinphoneListener
 * Created by 90Chris on 2016/4/6.
 */
public interface OnLinphoneListener {
    void onRegState(LinphoneCore.RegistrationState registrationState, String reason);
    void onCallState(LinphoneCall linphoneCall, LinphoneCall.State state, String reason);
}