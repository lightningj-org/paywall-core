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
package org.lightningj.paywall.lightninghandler;

import org.lightningj.paywall.JSONParsable;
import org.lightningj.paywall.vo.Invoice;

import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Object generated by LightningHandler whenever an invoice is added or settled in the underlying lightning node.
 *
 * Created by Philip Vendil on 2018-11-24.
 */
public class LightningEvent extends JSONParsable {

    protected LightningEventType type;
    protected Invoice invoice;
    protected LightningHandlerContext context;

    /**
     * Empty constructor
     */
    public LightningEvent(){}

    /**
     * Default constructor
     *
     * @param type the type of event, added or settled.
     * @param invoice the related invoice
     * @param context the latest context if the lightning handler.
     */
    public LightningEvent(LightningEventType type, Invoice invoice, LightningHandlerContext context) {
        this.type = type;
        this.invoice = invoice;
        this.context = context;
    }

    /**
     * JSON Parseable constructor
     *
     * @param jsonObject the json object to parse.
     */
    public LightningEvent(JsonObject jsonObject) throws JsonException {
        super(jsonObject);
    }

    /**
     *
     * @return the type of event, added or settled.
     */
    public LightningEventType getType() {
        return type;
    }

    /**
     *
     * @param type the type of event, added or settled.
     */
    public void setType(LightningEventType type) {
        this.type = type;
    }

    /**
     *
     * @return the related invoice
     */
    public Invoice getInvoice() {
        return invoice;
    }

    /**
     *
     * @param invoice the related invoice
     */
    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    /**
     *
     * @return the latest context of the used lightning handler that
     * contains the latest known state.
     */
    public LightningHandlerContext getContext() {
        return context;
    }

    /**
     *
     * @param context the latest context of the used lightning handler that
     * contains the latest known state.
     */
    public void setContext(LightningHandlerContext context) {
        this.context = context;
    }

    /**
     * Method that should set the objects property to Json representation.
     *
     * @param jsonObjectBuilder the json object build to use to set key/values in json
     * @throws JsonException if problems occurred converting object to JSON.
     */
    @Override
    public void convertToJson(JsonObjectBuilder jsonObjectBuilder) throws JsonException {
        if(type == null){
            throw new JsonException("Error building JSON object, required key type is null.");
        }
        add(jsonObjectBuilder,"type",type.name());
        add(jsonObjectBuilder,"invoice",invoice);
        addNotRequired(jsonObjectBuilder,"context",context);
    }

    /**
     * Method to read all properties from a JsonObject into this value object.
     *
     * @param jsonObject the json object to read key and values from and set object properties.
     * @throws JsonException if problems occurred converting object from JSON.
     */
    @Override
    public void parseJson(JsonObject jsonObject) throws JsonException {
        String typeValue = getString(jsonObject,"type", true);
        try{
            type = LightningEventType.valueOf(typeValue.toUpperCase());
        }catch (Exception e){
            if(e instanceof JsonException){
                throw (JsonException) e;
            }
            throw new JsonException("Error parsing JSON, invalid lightning event type " + typeValue + ".");
        }
        invoice = new Invoice(getJsonObject(jsonObject,"invoice",true));
        if(jsonObject.containsKey("context")) {
            context = LightningHandlerContext.parseContext(getJsonObject(jsonObject, "context", true));
        }
    }
}
