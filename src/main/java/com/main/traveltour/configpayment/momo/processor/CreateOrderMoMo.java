package com.main.traveltour.configpayment.momo.processor;

import com.main.traveltour.configpayment.momo.config.Environment;
import com.main.traveltour.configpayment.momo.enums.*;
import com.main.traveltour.configpayment.momo.models.HttpResponse;
import com.main.traveltour.configpayment.momo.models.PaymentRequest;
import com.main.traveltour.configpayment.momo.models.PaymentResponse;
import com.main.traveltour.configpayment.momo.shared.constants.Parameter;
import com.main.traveltour.configpayment.momo.shared.exception.MoMoException;
import com.main.traveltour.configpayment.momo.shared.utils.Encoder;
import com.main.traveltour.configpayment.momo.shared.utils.LogUtils;

/**
 * @author hainguyen
 * Documention: https://developers.momo.vn
 */
public class CreateOrderMoMo extends AbstractProcess<PaymentRequest, PaymentResponse> {

    public CreateOrderMoMo(Environment environment) {
        super(environment);
    }

    /**
     * Capture MoMo Process on Payment Gateway
     *
     * @param amount
     * @param extraData
     * @param orderInfo
     * @param env       name of the environment (dev or prod)
     * @param orderId   unique order ID in MoMo system
     * @param requestId request ID
     * @param returnURL URL to redirect customer
     * @param notifyURL URL for MoMo to return transaction status to merchant
     * @return PaymentResponse
     **/

    public static PaymentResponse process(Environment env, String orderId, String requestId, String amount, String orderInfo, String returnURL, String notifyURL, String extraData, com.main.traveltour.configpayment.momo.enums.RequestType requestType, Boolean autoCapture) throws Exception {
        try {
            CreateOrderMoMo m2Processor = new CreateOrderMoMo(env);

            PaymentRequest request = m2Processor.createPaymentCreationRequest(orderId, requestId, amount, orderInfo, returnURL, notifyURL, extraData, requestType, autoCapture);

            return m2Processor.execute(request);
        } catch (Exception exception) {
            LogUtils.error("[CreateOrderMoMoProcess] " + exception);
        }
        return null;
    }

    @Override
    public PaymentResponse execute(PaymentRequest request) throws MoMoException {
        try {

            String payload = getGson().toJson(request, PaymentRequest.class);

            HttpResponse response = execute.sendToMoMo(environment.getMomoEndpoint().getCreateUrl(), payload);

            if (response.getStatus() != 200) {
                throw new MoMoException("[PaymentResponse] [" + request.getOrderId() + "] -> Error API");
            }

            return getGson().fromJson(response.getData(), PaymentResponse.class);

        } catch (Exception exception) {
            LogUtils.error("[PaymentMoMoResponse] " + exception);
            throw new IllegalArgumentException("Invalid params capture MoMo Request");
        }
    }

    /**
     * @param orderId
     * @param requestId
     * @param amount
     * @param orderInfo
     * @param returnUrl
     * @param notifyUrl
     * @param extraData
     * @return
     */
    public PaymentRequest createPaymentCreationRequest(String orderId, String requestId, String amount, String orderInfo,
                                                       String returnUrl, String notifyUrl, String extraData, com.main.traveltour.configpayment.momo.enums.RequestType requestType, Boolean autoCapture) {

        try {
            String requestRawData = new StringBuilder()
                    .append(Parameter.ACCESS_KEY).append("=").append(partnerInfo.getAccessKey()).append("&")
                    .append(Parameter.AMOUNT).append("=").append(amount).append("&")
                    .append(Parameter.EXTRA_DATA).append("=").append(extraData).append("&")
                    .append(Parameter.IPN_URL).append("=").append(notifyUrl).append("&")
                    .append(Parameter.ORDER_ID).append("=").append(orderId).append("&")
                    .append(Parameter.ORDER_INFO).append("=").append(orderInfo).append("&")
                    .append(Parameter.PARTNER_CODE).append("=").append(partnerInfo.getPartnerCode()).append("&")
                    .append(Parameter.REDIRECT_URL).append("=").append(returnUrl).append("&")
                    .append(Parameter.REQUEST_ID).append("=").append(requestId).append("&")
                    .append(Parameter.REQUEST_TYPE).append("=").append(requestType.getRequestType())
                    .toString();

            String signRequest = Encoder.signHmacSHA256(requestRawData, partnerInfo.getSecretKey());

            return new PaymentRequest(partnerInfo.getPartnerCode(), orderId, requestId, Language.EN, orderInfo, Long.valueOf(amount), "test MoMo", null, requestType,
                    returnUrl, notifyUrl, "test store ID", extraData, null, autoCapture, null, signRequest);
        } catch (Exception e) {
            LogUtils.error("[PaymentRequest] " + e);
        }

        return null;
    }

}