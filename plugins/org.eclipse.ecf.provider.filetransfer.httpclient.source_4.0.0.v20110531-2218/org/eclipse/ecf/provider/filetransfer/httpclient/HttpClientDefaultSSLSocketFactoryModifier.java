/****************************************************************************
 * Copyright (c) 2004, 2009 Composent, Inc., IBM and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Composent, Inc. - initial API and implementation
 *  Henrich Kraemer - bug 263869, testHttpsReceiveFile fails using HTTP proxy
 *****************************************************************************/

package org.eclipse.ecf.provider.filetransfer.httpclient;

import java.io.IOException;
import java.net.Socket;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import org.eclipse.ecf.core.util.StringUtils;
import org.eclipse.ecf.filetransfer.events.socketfactory.INonconnectedSocketFactory;
import org.eclipse.ecf.internal.provider.filetransfer.httpclient.ISSLSocketFactoryModifier;

public class HttpClientDefaultSSLSocketFactoryModifier implements ISSLSocketFactoryModifier, INonconnectedSocketFactory {
	public static final String DEFAULT_SSL_PROTOCOL = "https.protocols"; //$NON-NLS-1$

	private SSLContext sslContext = null;

	private String defaultProtocolNames = System.getProperty(DEFAULT_SSL_PROTOCOL);

	public HttpClientDefaultSSLSocketFactoryModifier() {
		// empty
	}

	public SSLSocketFactory getSSLSocketFactory() throws IOException {
		if (null == sslContext) {
			try {
				sslContext = getSSLContext(defaultProtocolNames);
			} catch (Exception e) {
				IOException ioe = new IOException();
				ioe.initCause(e);
				throw ioe;
			}
		}
		return (sslContext == null) ? (SSLSocketFactory) SSLSocketFactory.getDefault() : sslContext.getSocketFactory();
	}

	public SSLContext getSSLContext(String protocols) {
		SSLContext rtvContext = null;

		if (protocols != null) {
			String protocolNames[] = StringUtils.split(protocols, ","); //$NON-NLS-1$
			for (int i = 0; i < protocolNames.length; i++) {
				try {
					rtvContext = SSLContext.getInstance(protocolNames[i]);
					sslContext.init(null, new TrustManager[] {new HttpClientSslTrustManager()}, null);
					break;
				} catch (Exception e) {
					// just continue
				}
			}
		}
		return rtvContext;
	}

	public Socket createSocket() throws IOException {
		return getSSLSocketFactory().createSocket();
	}

	public void dispose() {
		// empty
	}

	public INonconnectedSocketFactory getNonconnnectedSocketFactory() {
		return this;
	}

}
