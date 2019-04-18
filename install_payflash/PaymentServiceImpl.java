/**
 * Copyright (C) 1972-2019 SAP Co., Ltd. All rights reserved.
 */
package com.sap.sme.payment.service.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sap.sme.payment.service.*;
import com.sap.sme.payment.service.model.EmailEntity;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alipay.api.response.AlipayTradeQueryResponse;
import com.sap.sme.common.exception.BusinessException;
import com.sap.sme.common.json.util.JsonUtils;
import com.sap.sme.common.logger.LightLogger;
import com.sap.sme.common.util.DateTimeUtils;
import com.sap.sme.payment.api.domain.Currency;
import com.sap.sme.payment.api.domain.Order;
import com.sap.sme.payment.api.domain.Payment;
import com.sap.sme.payment.api.domain.PaymentMethod;
import com.sap.sme.payment.api.domain.Transaction;
import com.sap.sme.payment.api.domain.User;
import com.sap.sme.payment.api.util.CurrencyUtils;
import com.sap.sme.payment.api.util.PaymentMethodUtils;
import com.sap.sme.payment.data.dao.PfPaymentDao;
import com.sap.sme.payment.data.dao.X4PaymentAccountDao;
import com.sap.sme.payment.data.domain.PfPayment;
import com.sap.sme.payment.data.domain.X4PaymentAccount;
import com.sap.sme.payment.vendor.alipay.constant.AlipayApiConstants.TradeStatus;

/**
 * @author I311334
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    private final LightLogger log = LightLogger.getLogger(this);

    @Autowired
    private PfPaymentDao pfPaymentDao;

    @Autowired
    private X4PaymentAccountDao x4PaymentAccountDao;

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private PayPalService payPalService;

    @Autowired
    private EmailService emailService;
    
    @Autowired
    private TransactionService transactionService;

    @Autowired
    private EmailTemplateService emailTemplateService;

    @Autowired
    private PaymentNotifyService paymentNotifyService;


    /**
     * {@inheritDoc}
     */
    @Override
    public Payment get(String uuid) {
        PfPayment pfPayment = pfPaymentDao.getByUuid(UUID.fromString(uuid));
        if (pfPayment == null) {
            return null;
        }

        Payment payment = mapToPayment(pfPayment);
        normalizePaymentForQuery(payment);

        return payment;
    }
    
    @Override
    public Long getId(String uuid) {
        return pfPaymentDao.getByUuid(UUID.fromString(uuid)).getId();
    }

    @Override
    public Payment getByOrderId(String orderId) {
        List<PfPayment> pfPaymentList = pfPaymentDao.queryByOrderBusinessId(orderId);
        if (CollectionUtils.isEmpty(pfPaymentList)) {
            return null;
        }
        PfPayment pfPayment = pfPaymentList.get(0);
        Payment payment = mapToPayment(pfPayment);
        normalizePaymentForQuery(payment);

        return payment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Payment create(Payment payment) {
        normalizePaymentForCreate(payment);

        PfPayment pfPayment = mapToPfPayment(payment);
        pfPaymentDao.create(pfPayment);
        emailService.send(normalizeEmailForSend(payment));
        return payment;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void updatePaymentStatusFromRemote(String paymentUuid) {
        PfPayment pfPayment = pfPaymentDao.getByUuid(UUID.fromString(paymentUuid));
       
        if (pfPayment == null) {
            log.warn("Error update payment status from remote: Payment not exists. paymentUuid[", paymentUuid, "].");
            throw new BusinessException("Error update payment status from remote: Payment not exists. paymentUuid[", paymentUuid, "].");
        }
        String paymentMethod = transactionService.getByPaymentId(pfPayment.getId()).getPaymentMethod();
        if (StringUtils.equals(paymentMethod, PaymentMethod.Type.ALIPAY)) {
            AlipayTradeQueryResponse alipayResponse = alipayService.queryTrade(
                    StringUtils.isNotBlank(pfPayment.getOrderBusinessId()) ? pfPayment.getOrderBusinessId() : pfPayment.getUuid().toString());

            if (StringUtils.equals(alipayResponse.getTradeStatus(), TradeStatus.TRADE_SUCCESS)) {
            	pfPayment.setPaymentMethodList(PaymentMethod.Type.ALIPAY);
                pfPayment.setStatus(Payment.Status.PAID);
                pfPaymentDao.update(pfPayment);
            }
        }

        if (StringUtils.equals(paymentMethod, PaymentMethod.Type.PAYFLOWLINK)) {
            boolean resultpaid = payPalService.queryPayflowLinkPay(pfPayment.getUuid().toString());
            if (resultpaid == true) {
                pfPayment.setPaymentMethodList(PaymentMethod.Type.PAYFLOWLINK);  
                pfPayment.setStatus(Payment.Status.PAID);
                pfPaymentDao.update(pfPayment);
            }
        }

        Payment payment = mapToPayment(pfPayment);
        String paymentBusinessId = paymentNotifyService.notify(payment);
        pfPayment.setBusinessId(paymentBusinessId);
        pfPaymentDao.update(pfPayment);
    }


    private void normalizePaymentForCreate(Payment payment) {
        String uuid = UUID.randomUUID().toString();
        Timestamp currentTime = DateTimeUtils.currentTimestamp();

        payment.setCreateTime(currentTime);
        payment.setUpdateTime(currentTime);

        payment.setUuid(uuid);
        payment.setStatus(Payment.Status.UNPAID);

        payment.setUrl("http://payflash.kkops.cc/payment/" + uuid);

        CurrencyUtils.normalize(payment);
        PaymentMethodUtils.normalize(payment);
    }

    private void normalizePaymentForQuery(Payment payment) {
        CurrencyUtils.normalize(payment);
        PaymentMethodUtils.normalize(payment);
    }

    private EmailEntity normalizeEmailForSend(Payment payment){
        EmailEntity email = new EmailEntity();
        email.setFrom("noreply@payflash.kkops.cc");
        email.setTo("kongdengyuan@gmail.com,dengyuan.kong@sap.com");
        email.setHtml(true);
        email.setSubject("Please pay your invoice");
        String body = emailTemplateService.generateContenetWithDbTemplate(payment);
        email.setBody(body);
        email.setNeedToRetry(true);
        email.setRetryCount(3);
        return email;
    }

    private PfPayment mapToPfPayment(Payment payment) {
        User payee = payment.getPayee();
        Order order = payment.getOrder();

        PfPayment pfPayment = new PfPayment();

        pfPayment.setCreateTime(payment.getCreateTime());
        pfPayment.setUpdateTime(payment.getUpdateTime());
        pfPayment.setUuid(UUID.fromString(payment.getUuid()));
        pfPayment.setBusinessId(payment.getBusinessId());
        pfPayment.setOrderBusinessId((order != null) ? order.getBusinessId() : null);
        pfPayment.setUrl(payment.getUrl());
        pfPayment.setLocale(payment.getLocale());
        pfPayment.setCurrencyCode(toCurrencyCode(payment.getCurrency()));
        pfPayment.setAmount(payment.getAmount());
        pfPayment.setPaymentMethodList(toPaymentMethodListStr(payment.getPaymentMethodList()));
        pfPayment.setStatus(payment.getStatus());
        pfPayment.setPayeeJson(JsonUtils.toJson(payee));
        pfPayment.setTitle(payment.getTitle());
        pfPayment.setOrderJson(JsonUtils.toJson(order));

        return pfPayment;
    }

    private Payment mapToPayment(PfPayment pfPayment) {
        Payment payment = new Payment();

        payment.setCreateTime(pfPayment.getCreateTime());
        payment.setUpdateTime(pfPayment.getUpdateTime());
        payment.setUuid(pfPayment.getUuid().toString());
        payment.setBusinessId(pfPayment.getBusinessId());
        payment.setUrl(pfPayment.getUrl());
        payment.setLocale(pfPayment.getLocale());
        payment.setCurrency(toCurrency(pfPayment.getCurrencyCode()));
        payment.setAmount(pfPayment.getAmount());
        payment.setPaymentMethodList(toPaymentMethodList(pfPayment.getPaymentMethodList()));
        payment.setStatus(pfPayment.getStatus());
        payment.setPayee(JsonUtils.toObject(pfPayment.getPayeeJson(), User.class));
        payment.setTitle(payment.getTitle());
        payment.setOrder(JsonUtils.toObject(pfPayment.getOrderJson(), Order.class));

        return payment;
    }

    private String toPaymentMethodListStr(List<PaymentMethod> paymentMethodList) {
        StringBuilder buf = new StringBuilder();

        if (CollectionUtils.isNotEmpty(paymentMethodList)) {
            for (PaymentMethod paymentMethod : paymentMethodList) {
                if (buf.length() > 0) {
                    buf.append(",");
                }
                buf.append(paymentMethod.getType());
            }
        }

        return buf.toString();
    }

    private List<PaymentMethod> toPaymentMethodList(String paymentMethodListStr) {
        List<PaymentMethod> paymentMethodList = new ArrayList<>();

        if (StringUtils.isNotBlank(paymentMethodListStr)) {
            for (String paymentMethodStr : StringUtils.split(paymentMethodListStr, ",")) {
                if (StringUtils.isBlank(paymentMethodStr)) {
                    continue;
                }

                PaymentMethod paymentMethod = new PaymentMethod();
                paymentMethod.setType(paymentMethodStr);

                // Temp solution.
                if (StringUtils.equals(paymentMethodStr, PaymentMethod.Type.STRIPE)) {
                    X4PaymentAccount x4PaymentAccount = x4PaymentAccountDao.getFirstByType(PaymentMethod.Type.STRIPE);
                    if (x4PaymentAccount != null) {
                        paymentMethod.addProperty("stripePublishKey", x4PaymentAccount.getStripePublishKey());
                    }
                }

                paymentMethodList.add(paymentMethod);
            }
        }

        return paymentMethodList;
    }

    private String toCurrencyCode(Currency currency) {
        if (currency == null) {
            return null;
        }
        return currency.getCode();
    }

    private Currency toCurrency(String currencyCode) {
        Currency currency = new Currency();
        currency.setCode(currencyCode);
        return currency;
    }

    /**
     * @return the pfPaymentDao
     */
    public PfPaymentDao getPfPaymentDao() {
        return pfPaymentDao;
    }

    /**
     * @param pfPaymentDao the pfPaymentDao to set
     */
    public void setPfPaymentDao(PfPaymentDao pfPaymentDao) {
        this.pfPaymentDao = pfPaymentDao;
    }

    /**
     * @return the x4PaymentAccountDao
     */
    public X4PaymentAccountDao getX4PaymentAccountDao() {
        return x4PaymentAccountDao;
    }

    /**
     * @param x4PaymentAccountDao the x4PaymentAccountDao to set
     */
    public void setX4PaymentAccountDao(X4PaymentAccountDao x4PaymentAccountDao) {
        this.x4PaymentAccountDao = x4PaymentAccountDao;
    }

    /**
     * @return the alipayService
     */
    public AlipayService getAlipayService() {
        return alipayService;
    }

    /**
     * @param alipayService the alipayService to set
     */
    public void setAlipayService(AlipayService alipayService) {
        this.alipayService = alipayService;
    }

}
