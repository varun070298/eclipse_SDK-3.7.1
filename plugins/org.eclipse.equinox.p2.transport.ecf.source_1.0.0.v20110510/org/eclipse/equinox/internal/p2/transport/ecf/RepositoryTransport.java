/*******************************************************************************
 * Copyright (c) 2006, 2011, IBM Corporation and other.
 * The code, documentation and other materials contained herein have been
 * licensed under the Eclipse Public License - v 1.0 by the copyright holder
 * listed above, as the Initial Contributor under such license. The text of
 * such license is available at www.eclipse.org.
 * 
 * Contributors
 * 	IBM Corporation - Initial API and implementation.
 *  Cloudsmith Inc - Implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.p2.transport.ecf;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ecf.core.identity.IDCreateException;
import org.eclipse.ecf.core.security.ConnectContextFactory;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.filetransfer.BrowseFileTransferException;
import org.eclipse.ecf.filetransfer.IncomingFileTransferException;
import org.eclipse.ecf.filetransfer.UserCancelledException;
import org.eclipse.equinox.internal.p2.repository.AuthenticationFailedException;
import org.eclipse.equinox.internal.p2.repository.Credentials;
import org.eclipse.equinox.internal.p2.repository.Credentials.LoginCanceledException;
import org.eclipse.equinox.internal.p2.repository.DownloadStatus;
import org.eclipse.equinox.internal.p2.repository.FileInfo;
import org.eclipse.equinox.internal.p2.repository.JREHttpClientRequiredException;
import org.eclipse.equinox.internal.p2.repository.Messages;
import org.eclipse.equinox.internal.p2.repository.RepositoryPreferences;
import org.eclipse.equinox.internal.p2.repository.Transport;
import org.eclipse.equinox.internal.provisional.p2.repository.IStateful;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.core.UIServices.AuthenticationInfo;
import org.eclipse.equinox.p2.core.spi.IAgentServiceFactory;
import org.eclipse.osgi.util.NLS;

/**
 * RepositoryTransport adapts p2 to ECF file download and file browsing.
 * Download is performed by {@link FileReader}, and file browsing is performed by
 * {@link FileInfoReader}.
 */
public class RepositoryTransport extends Transport implements IAgentServiceFactory {
	private static RepositoryTransport instance;

	/**
	 * Returns an shared instance of Generic Transport
	 */
	//	public static synchronized RepositoryTransport getInstance() {
	//		if (instance == null) {
	//			instance = new RepositoryTransport();
	//		}
	//		return instance;
	//	}

	public IStatus download(URI toDownload, OutputStream target, long startPos, IProgressMonitor monitor) {

		boolean promptUser = false;
		boolean useJREHttp = false;
		AuthenticationInfo loginDetails = null;
		for (int i = RepositoryPreferences.getLoginRetryCount(); i > 0; i--) {
			FileReader reader = null;
			try {
				loginDetails = Credentials.forLocation(toDownload, promptUser, loginDetails);
				IConnectContext context = (loginDetails == null) ? null : ConnectContextFactory.createUsernamePasswordConnectContext(loginDetails.getUserName(), loginDetails.getPassword());

				// perform the download
				reader = new FileReader(context);
				reader.readInto(toDownload, target, startPos, monitor);

				// check that job ended ok - throw exceptions otherwise
				IStatus result = reader.getResult();
				if (result == null) {
					String msg = NLS.bind(Messages.RepositoryTransport_failedReadRepo, toDownload);
					DownloadStatus ds = new DownloadStatus(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, msg, null);
					return statusOn(target, ds, reader);
				}
				if (result.getSeverity() == IStatus.CANCEL)
					throw new OperationCanceledException();
				if (!result.isOK())
					throw new CoreException(result);

				// Download status is expected on success
				DownloadStatus status = new DownloadStatus(IStatus.OK, Activator.ID, Status.OK_STATUS.getMessage());
				return statusOn(target, status, reader);
			} catch (UserCancelledException e) {
				statusOn(target, new DownloadStatus(IStatus.CANCEL, Activator.ID, 1, "", null), reader); //$NON-NLS-1$
				throw new OperationCanceledException();
			} catch (OperationCanceledException e) {
				statusOn(target, new DownloadStatus(IStatus.CANCEL, Activator.ID, 1, "", null), reader); //$NON-NLS-1$
				throw e;
			} catch (CoreException e) {
				if (e.getStatus().getException() == null)
					return statusOn(target, forException(e, toDownload), reader);
				return statusOn(target, forStatus(e.getStatus(), toDownload), reader);
			} catch (FileNotFoundException e) {
				return statusOn(target, forException(e, toDownload), reader);
			} catch (AuthenticationFailedException e) {
				promptUser = true;
			} catch (Credentials.LoginCanceledException e) {
				DownloadStatus status = new DownloadStatus(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_AUTHENTICATION, //
						NLS.bind(Messages.UnableToRead_0_UserCanceled, toDownload), null);
				return statusOn(target, status, null);
			} catch (JREHttpClientRequiredException e) {
				if (!useJREHttp) {
					useJREHttp = true; // only do this once
					i++; // need an extra retry
					Activator.getDefault().useJREHttpClient();
				}
			}
		}
		// reached maximum number of retries without success
		DownloadStatus status = new DownloadStatus(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_AUTHENTICATION, //
				NLS.bind(Messages.UnableToRead_0_TooManyAttempts, toDownload), null);
		return statusOn(target, status, null);
	}

	public IStatus download(URI toDownload, OutputStream target, IProgressMonitor monitor) {
		return download(toDownload, target, -1, monitor);
	}

	public InputStream stream(URI toDownload, IProgressMonitor monitor) throws FileNotFoundException, CoreException, AuthenticationFailedException {

		boolean promptUser = false;
		boolean useJREHttp = false;
		AuthenticationInfo loginDetails = null;
		for (int i = RepositoryPreferences.getLoginRetryCount(); i > 0; i--) {
			FileReader reader = null;
			try {
				loginDetails = Credentials.forLocation(toDownload, promptUser, loginDetails);
				IConnectContext context = (loginDetails == null) ? null : ConnectContextFactory.createUsernamePasswordConnectContext(loginDetails.getUserName(), loginDetails.getPassword());

				// perform the streamed download
				reader = new FileReader(context);
				return reader.read(toDownload, monitor);
			} catch (UserCancelledException e) {
				throw new OperationCanceledException();
			} catch (AuthenticationFailedException e) {
				promptUser = true;
			} catch (CoreException e) {
				// must translate this core exception as it is most likely not informative to a user
				if (e.getStatus().getException() == null)
					throw new CoreException(RepositoryStatus.forException(e, toDownload));
				throw new CoreException(RepositoryStatus.forStatus(e.getStatus(), toDownload));
			} catch (LoginCanceledException e) {	
				// i.e. same behavior when user cancels as when failing n attempts.
				throw new AuthenticationFailedException();
			} catch (JREHttpClientRequiredException e) {
				if (!useJREHttp) {
					useJREHttp = true; // only do this once
					i++; // need an extra retry
					Activator.getDefault().useJREHttpClient();
				}
			}
		}
		throw new AuthenticationFailedException();
	}

	/**
	 * Set the status on the output stream if it implements IStateful. 
	 * Update the DownloadStatus with information from FileReader.
	 * @param target an OutputStream possibly implementing IStateful
	 * @param status a DownloadStatus configured with status message, code, etc
	 * @param reader a FileReade that was used to download (or null if not known).
	 * @throws OperationCanceledException if the operation was canceled by the user.
	 * @return the configured DownloadStatus status.
	 */
	private static DownloadStatus statusOn(OutputStream target, DownloadStatus status, FileReader reader) {
		if (reader != null) {
			FileInfo fi = reader.getLastFileInfo();
			if (fi != null) {
				status.setFileSize(fi.getSize());
				status.setLastModified(fi.getLastModified());
				status.setTransferRate(fi.getAverageSpeed());
			}
		}
		if (target instanceof IStateful)
			((IStateful) target).setStatus(status);
		return status;
	}

	public long getLastModified(URI toDownload, IProgressMonitor monitor) throws CoreException, FileNotFoundException, AuthenticationFailedException {
		boolean promptUser = false;
		boolean useJREHttp = false;
		AuthenticationInfo loginDetails = null;
		for (int i = RepositoryPreferences.getLoginRetryCount(); i > 0; i--) {
			try {
				loginDetails = Credentials.forLocation(toDownload, promptUser, loginDetails);
				IConnectContext context = (loginDetails == null) ? null : ConnectContextFactory.createUsernamePasswordConnectContext(loginDetails.getUserName(), loginDetails.getPassword());
				// get the remote info
				FileInfoReader reader = new FileInfoReader(context);
				return reader.getLastModified(toDownload, monitor);
			} catch (UserCancelledException e) {
				throw new OperationCanceledException();
			} catch (CoreException e) {
				// must translate this core exception as it is most likely not informative to a user
				if (e.getStatus().getException() == null)
					throw new CoreException(RepositoryStatus.forException(e, toDownload));
				throw new CoreException(RepositoryStatus.forStatus(e.getStatus(), toDownload));
			} catch (AuthenticationFailedException e) {
				promptUser = true;
			} catch (LoginCanceledException e) {
				// same behavior as if user failed n attempts.
				throw new AuthenticationFailedException();
			} catch (JREHttpClientRequiredException e) {
				if (!useJREHttp) {
					useJREHttp = true; // only do this once
					i++; // need an extra retry
					Activator.getDefault().useJREHttpClient();
				}
			}

		}
		// reached maximum number of authentication retries without success
		throw new AuthenticationFailedException();
	}

	public static DownloadStatus forStatus(IStatus original, URI toDownload) {
		Throwable t = original.getException();
		return forException(t, toDownload);
	}

	public static DownloadStatus forException(Throwable t, URI toDownload) {
		if (t instanceof FileNotFoundException || (t instanceof IncomingFileTransferException && ((IncomingFileTransferException) t).getErrorCode() == 404))
			return new DownloadStatus(IStatus.ERROR, Activator.ID, ProvisionException.ARTIFACT_NOT_FOUND, NLS.bind(Messages.artifact_not_found, toDownload), t);
		if (t instanceof ConnectException)
			return new DownloadStatus(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_FAILED_READ, NLS.bind(Messages.TransportErrorTranslator_UnableToConnectToRepository_0, toDownload), t);
		if (t instanceof UnknownHostException)
			return new DownloadStatus(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_INVALID_LOCATION, NLS.bind(Messages.TransportErrorTranslator_UnknownHost, toDownload), t);
		if (t instanceof IDCreateException) {
			IStatus status = ((IDCreateException) t).getStatus();
			if (status != null && status.getException() != null)
				t = status.getException();

			return new DownloadStatus(IStatus.ERROR, Activator.ID, ProvisionException.REPOSITORY_INVALID_LOCATION, NLS.bind(Messages.TransportErrorTranslator_MalformedRemoteFileReference, toDownload), t);
		}
		int code = 0;

		// default to report as read repository error
		int provisionCode = ProvisionException.REPOSITORY_FAILED_READ;

		if (t instanceof IncomingFileTransferException)
			code = ((IncomingFileTransferException) t).getErrorCode();
		else if (t instanceof BrowseFileTransferException)
			code = ((BrowseFileTransferException) t).getErrorCode();

		// Switch on error codes in the HTTP error code range. 
		// Note that 404 uses ARTIFACT_NOT_FOUND (as opposed to REPOSITORY_NOT_FOUND, which
		// is determined higher up in the calling chain).
		if (code == 401)
			provisionCode = ProvisionException.REPOSITORY_FAILED_AUTHENTICATION;
		else if (code == 404)
			provisionCode = ProvisionException.ARTIFACT_NOT_FOUND;

		// Add more specific translation here

		return new DownloadStatus(IStatus.ERROR, Activator.ID, provisionCode, //
				code == 0 ? NLS.bind(Messages.io_failedRead, toDownload) //
						: RepositoryStatus.codeToMessage(code, toDownload.toString()), t);
	}

	public Object createService(IProvisioningAgent agent) {
		if (instance ==  null)
			return instance;
		return instance;
	}
}
