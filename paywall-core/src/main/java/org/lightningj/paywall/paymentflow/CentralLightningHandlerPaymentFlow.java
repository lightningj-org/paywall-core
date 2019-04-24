/*
 *************************************************************************
 *                                                                       *
 *  LightningJ                                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public License   *
 *  (LGPL-3.0-or-later)                                                  *
 *  License as published by the Free Software Foundation; either         *
 *  version 3 of the License, or any later version.                      *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.lightningj.paywall.paymentflow;

import org.jose4j.jwt.JwtClaims;
import org.lightningj.paywall.AlreadyExecutedException;
import org.lightningj.paywall.InternalErrorException;
import org.lightningj.paywall.annotations.PaymentRequired;
import org.lightningj.paywall.currencyconverter.CurrencyConverter;
import org.lightningj.paywall.currencyconverter.InvalidCurrencyException;
import org.lightningj.paywall.keymgmt.Context;
import org.lightningj.paywall.lightninghandler.LightningHandler;
import org.lightningj.paywall.paymenthandler.PaymentHandler;
import org.lightningj.paywall.requestpolicy.RequestPolicy;
import org.lightningj.paywall.requestpolicy.RequestPolicyFactory;
import org.lightningj.paywall.tokengenerator.TokenContext;
import org.lightningj.paywall.tokengenerator.TokenException;
import org.lightningj.paywall.tokengenerator.TokenGenerator;
import org.lightningj.paywall.util.Base64Utils;
import org.lightningj.paywall.vo.*;
import org.lightningj.paywall.vo.amount.CryptoAmount;
import org.lightningj.paywall.web.CachableHttpServletRequest;

import java.io.IOException;
import java.time.Duration;


/**
 * Payment flow for use case there is one central node managing lightning invoices from
 * orders generated by multiple paywalled applications. In this case have each paywalled
 * application it's PaymentHandler and the central node have LightningHandler and
 * CurrencyConverter.
 * <p>
 * Should only be created by a BasePaymentFlowManager implementation.
 *
 * Created by Philip Vendil on 2019-01-01.
 */
public class CentralLightningHandlerPaymentFlow extends BasePaymentFlow {

    protected String centralSystemRecipientId;
    protected boolean registerNew;

    /**
     * Default constructor for CentralLightningHandlerPaymentFlow, might be initialized
     * differenlty given if flow state is in a pay walled node or central lightning handler node.
     *
     * @param paymentRequired the annotation signaling the requested resource requires payment.
     *                        Can be null for some nodes in a distributed setup.
     * @param request the related HTTP Request in this phase of the payment flow.
     * @param orderRequest the orderRequest calculated either from paymentRequired annotation
     *                     of extracted from JWT token depending on state in the payment flow.
     * @param requestPolicyFactory the used RequestPolicyFactory. Might be null for nodes in a distributed setup.
     * @param lightningHandler the used LightningHandler. Might be null for nodes in a distributed setup.
     * @param paymentHandler the used PaymentHandler. Might be null for nodes in a distributed setup.
     * @param tokenGenerator the user TokenGenerator, should never be null.
     * @param currencyConverter the used CurrencyConverter. Might be null for nodes in a distributed setup.
     * @param tokenClaims all claims parsed from the related JWT token. Null in no related token exists in current state.
     * @param expectedTokenType the expected type of JWT token expected in this state of the payment flow.
     * @param notBeforeDuration the duration for the not before field in generated
     *                          JWT tokens. This can be positive if it should be valid in the future, or negative
     *                          to support skewed clocked between systems. Use null if no not before date should
     *                          be set in generated JWT tokens.
     * @param centralSystemRecipientId the recipient id of the central lightning handler node used to encrypt JWT tokens to.
     * @param registerNew in settled invoices that doesn't have prior order created should automatically be registerd.
     */
    public CentralLightningHandlerPaymentFlow(PaymentRequired paymentRequired, CachableHttpServletRequest request, OrderRequest orderRequest, RequestPolicyFactory requestPolicyFactory,
                                              LightningHandler lightningHandler, PaymentHandler paymentHandler, TokenGenerator tokenGenerator, CurrencyConverter currencyConverter,
                                              JwtClaims tokenClaims, ExpectedTokenType expectedTokenType, Duration notBeforeDuration, String centralSystemRecipientId,
                                              boolean registerNew) {
        super(paymentRequired, request, orderRequest,
                requestPolicyFactory, lightningHandler,
                paymentHandler, tokenGenerator, currencyConverter,
                tokenClaims, expectedTokenType, notBeforeDuration);

        this.centralSystemRecipientId = centralSystemRecipientId;
        this.registerNew = registerNew;
        assert centralSystemRecipientId != null : "Internal error, recipient id of central system used to encrypt JWT tokens to must be set.";
    }

    /**
     * Method to create and order and an invoice. In this setup is the this method
     * called twice first by the paywalled node to create the order and then by the
     * node with lightning handler to create the invoice to simulate the workflow
     * of the local payment flow. The method automatically detects which state the flow
     * is in.
     *
     * @return a value object containing a payment or invoice JWT Token and optionally and invoice.
     * @throws IllegalArgumentException if user specified parameters (used by the constructor) was invalid.
     * @throws IOException if communication problems occurred with underlying components.
     * @throws InternalErrorException if internal errors occurred processing the method.
     * @throws InvalidCurrencyException if problems occurred converting the currency in the order to the one
     * used in the invoice.
     * @throws TokenException if problems occurred generating or validating related JWT Token.
     */
    @Override
    public InvoiceResult requestPayment() throws IllegalArgumentException, IOException, InternalErrorException, InvalidCurrencyException, TokenException{
        if(paymentRequired == null){
            return generateInvoice();
        }else{
            return generateOrder();
        }

    }

    /**
     * Help method called by requestPayment on a pay walled node to generate a payment token
     * containing all relevant order data in order for the lightning node to create an invoice.
     *
     * @return a value object containing a payment JWT Token and no invoice set.
     * @throws IllegalArgumentException if user specified parameters (used by the constructor) was invalid.
     * @throws IOException if communication problems occurred with underlying components.
     * @throws InternalErrorException if internal errors occurred processing the method.
     * @throws InvalidCurrencyException if problems occurred converting the currency in the order to the one
     * used in the invoice.
     * @throws TokenException if problems occurred generating or validating related JWT Token.
     */
    protected InvoiceResult generateOrder() throws IllegalArgumentException, IOException, InternalErrorException, InvalidCurrencyException, TokenException{
        assert getPaymentHandler() != null  : "Internal error, configured PaymentHandler cannot be null in central lightning handler payment flow";
        assert getRequestPolicyFactory() != null  : "Internal error, configured RequestPolicyFactory cannot be null in central lightning handler payment flow";
        assert getTokenGenerator() != null  : "Internal error, configured TokenGenerator cannot be null in central lightning handler payment flow";

        RequestPolicy requestPolicy = getRequestPolicyFactory().getRequestPolicy(paymentRequired);
        requestData = requestPolicy.significantRequestDataDigest(request);

        PreImageData preImageData = getTokenGenerator().genPreImageData();
        Order order = getPaymentHandler().createOrder(preImageData.getPreImageHash(), orderRequest);
        PreImageOrder preImageOrder = new PreImageOrder(preImageData.getPreImage(),order);
        preImageHash = preImageOrder.getPreImageHash();
        String orderToken = getTokenGenerator().generatePaymentToken(orderRequest,preImageOrder,requestData,order.getExpireDate(), getNotBeforeDate(),centralSystemRecipientId );

        return new InvoiceResult(null,orderToken);
    }

    /**
     * Help method to generate invoice on a lightning handler node given order in payment JWT token.
     *
     * @return a value object containing a invoice JWT Token and generated invoice.
     * @throws IllegalArgumentException if user specified parameters (used by the constructor) was invalid.
     * @throws IOException if communication problems occurred with underlying components.
     * @throws InternalErrorException if internal errors occurred processing the method.
     * @throws InvalidCurrencyException if problems occurred converting the currency in the order to the one
     * used in the invoice.
     * @throws TokenException if problems occurred generating or validating related JWT Token.
     */
    protected InvoiceResult generateInvoice() throws IllegalArgumentException, IOException, InternalErrorException, InvalidCurrencyException, TokenException{
        assert expectedTokenType == ExpectedTokenType.PAYMENT_TOKEN : "Invalid expected payment token type specified: " + expectedTokenType;
        assert getLightningHandler() != null : "Internal error, configured LightningHandler cannot be null in central lightning handler payment flow";
        assert getTokenGenerator() != null  : "Internal error, configured TokenGenerator cannot be null in central lightning handler payment flow";
        assert getCurrencyConverter() != null  : "Internal error, configured CurrencyConverter cannot be null in central lightning handler payment flow";

        CryptoAmount convertedAmount = getCurrencyConverter().convert(order.getOrderAmount());
        ConvertedOrder convertedOrder = new ConvertedOrder(order,convertedAmount);

        Invoice invoice = getLightningHandler().generateInvoice(order.toPreImageData(),convertedOrder);
        invoice.setSourceNode(getTokenIssuer());
        String invoiceToken = getTokenGenerator().generateInvoiceToken(orderRequest,invoice,requestData,invoice.getExpireDate(), getNotBeforeDate(),centralSystemRecipientId);

        return new InvoiceResult(invoice, invoiceToken);
    }

    /**
     * Method to check if a invoice is settled and returns an InvoiceResult if it is settled, otherwise null.
     * @return InvoiceResult with invoice token if related token is settled, otherwise null.
     * @throws IllegalArgumentException if user specified parameters (used by the constructor) was invalid.
     * @throws IOException if communication problems occurred with underlying components.
     * @throws InternalErrorException if internal errors occurred processing the method.
     * @throws TokenException if problem occurred generating the settlement token.
     */
    public InvoiceResult checkSettledInvoice() throws IllegalArgumentException, IOException, InternalErrorException, TokenException{
        assert getLightningHandler() != null;
        assert invoice != null;

        String sourceNode = invoice.getSourceNode();
        invoice = getLightningHandler().lookupInvoice(invoice.getPreImageHash());
        if(invoice.isSettled()){
            String invoiceToken = getTokenGenerator().generateInvoiceToken(orderRequest,invoice,requestData,invoice.getExpireDate(), getNotBeforeDate(),sourceNode);
            return new InvoiceResult(invoice, invoiceToken);
        }

        return null;
    }

    /**
     * Method to check if related invoice is settled by the end user.
     *
     * @return true if settled.
     *
     * @throws IllegalArgumentException if user specified parameters (used by the constructor) was invalid.
     */
    @Override
    public boolean isSettled() throws IllegalArgumentException {
        if(invoice == null){
            throw new IllegalArgumentException("No Invoice cookie found in request.");
        }
        return invoice.isSettled();
    }

    /**
     * Method to retrieve a settlement and generate a settlement token.
     *
     * @return a value object containing the settlement and the related settlement token.
     * @throws IllegalArgumentException if user specified parameters (used by the constructor) was invalid.
     * @throws IOException if communication problems occurred with underlying components.
     * @throws InternalErrorException if internal errors occurred processing the method.
     * @throws TokenException if problem occurred generating the settlement token.
     */
    @Override
    public SettlementResult getSettlement() throws IllegalArgumentException, IOException, InternalErrorException, TokenException {
        if(invoice == null){
            throw new IllegalArgumentException("No Invoice cookie found in request.");
        }
        if(invoice.isSettled()){
            settlement = getPaymentHandler().registerSettledInvoice(invoice,registerNew,orderRequest,null);

            String destinationId = getTokenGenerator().getIssuerName(TokenContext.CONTEXT_SETTLEMENT_TOKEN_TYPE);
            String token = getTokenGenerator().generateSettlementToken(orderRequest,settlement,requestData,settlement.getValidUntil(),settlement.getValidFrom(), destinationId);
            return new SettlementResult(settlement,token);
        }else{
            throw new IllegalArgumentException("Related Invoice " + Base64Utils.encodeBase64String(invoice.getPreImageHash()) + " haven't been settled.");
        }
    }

    /**
     *
     * @return the source node value set in the invoice claim, this method should
     * only be called during certain states in the payment flow and by the node
     * with the Lightning handler.
     */
    @Override
    protected String getSourceNode() {
        assert invoice != null : "Invoice should be set before generating a settlement in a distributed setup.";
        return invoice.getSourceNode();
    }
}
