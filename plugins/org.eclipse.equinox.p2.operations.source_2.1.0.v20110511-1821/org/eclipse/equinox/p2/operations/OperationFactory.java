/*******************************************************************************
 *  Copyright (c) 2011 Sonatype, Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Sonatype, Inc. - initial API and implementation
 *     IBM Corporation - Ongoing development
 *******************************************************************************/
package org.eclipse.equinox.p2.operations;

import java.net.URI;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.p2.operations.Activator;
import org.eclipse.equinox.internal.p2.operations.Messages;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.*;
import org.eclipse.equinox.p2.engine.query.UserVisibleRootQuery;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.equinox.p2.query.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * OperationFactory provides a set of helpers to simplify dealing with the running installation.
 * Among other things, it simplifies the installation, un-installation and update.
 * If the system you are trying to modify is not the running one, you need to directly use the various subclass of {@link ProfileChangeOperation}.
 * @since 2.1  
 */
public class OperationFactory {

	private IProvisioningAgent getAgent() {
		Collection<ServiceReference<IProvisioningAgent>> ref = null;
		try {
			ref = Activator.getContext().getServiceReferences(IProvisioningAgent.class, '(' + IProvisioningAgent.SERVICE_CURRENT + '=' + Boolean.TRUE.toString() + ')');
		} catch (InvalidSyntaxException e) {
			//ignore can't happen since we write the filter ourselves
		}
		if (ref == null || ref.size() == 0)
			throw new IllegalStateException(Messages.OperationFactory_noAgent);
		IProvisioningAgent agent = Activator.getContext().getService(ref.iterator().next());
		Activator.getContext().ungetService(ref.iterator().next());
		return agent;
	}

	//Return a list of IUs from the list of versionedIDs originally provided
	private Collection<IInstallableUnit> gatherIUs(IQueryable<IInstallableUnit> searchContext, Collection<IVersionedId> ius, boolean checkIUs, IProgressMonitor monitor) throws ProvisionException {
		Collection<IInstallableUnit> gatheredIUs = new ArrayList<IInstallableUnit>(ius.size());

		for (IVersionedId versionedId : ius) {
			if (!checkIUs && versionedId instanceof IInstallableUnit) {
				gatheredIUs.add((IInstallableUnit) versionedId);
				continue;
			}

			IQuery<IInstallableUnit> installableUnits = QueryUtil.createIUQuery(versionedId.getId(), versionedId.getVersion());
			IQueryResult<IInstallableUnit> matches = searchContext.query(installableUnits, monitor);
			if (matches.isEmpty())
				throw new ProvisionException(new Status(IStatus.ERROR, Activator.ID, NLS.bind(Messages.OperationFactory_noIUFound, versionedId)));

			//Add the first IU
			Iterator<IInstallableUnit> iuIt = matches.iterator();
			gatheredIUs.add(iuIt.next());
		}
		return gatheredIUs;
	}

	private ProvisioningContext createProvisioningContext(Collection<URI> repos, IProvisioningAgent agent) {
		ProvisioningContext ctx = new ProvisioningContext(agent);
		if (repos != null) {
			ctx.setMetadataRepositories(repos.toArray(new URI[repos.size()]));
			ctx.setArtifactRepositories(repos.toArray(new URI[repos.size()]));
		}
		return ctx;
	}

	/**
	 * This factory method creates an {@link InstallOperation} to install all the elements listed from the specified repositories. 
	 * @param toInstall the elements to install. This can not be null.
	 * @param repos the repositories to install the elements from. 
	 * @param monitor the progress monitor
	 * @return an operation to install
	 */
	public InstallOperation createInstallOperation(Collection<IVersionedId> toInstall, Collection<URI> repos, IProgressMonitor monitor) throws ProvisionException {
		Assert.isNotNull(toInstall);
		IProvisioningAgent agent = getAgent();

		//add the repos
		ProvisioningContext ctx = createProvisioningContext(repos, agent);

		//find the ius to install and create the operation
		InstallOperation resultingOperation = new InstallOperation(new ProvisioningSession(agent), gatherIUs(ctx.getMetadata(monitor), toInstall, false, monitor));
		resultingOperation.setProvisioningContext(ctx);
		resultingOperation.setProfileId(IProfileRegistry.SELF);

		return resultingOperation;
	}

	/**
	 * Create an {@link UninstallOperation} that will uninstall the listed elements from the running instance. 
	 * @param toUninstall the elements to uninstall. This can not be null.
	 * @param repos the repositories to install the elements from. Passing null will 
	 * @param monitor the progress monitor
	 * @return an operation to uninstall
	 */
	public UninstallOperation createUninstallOperation(Collection<IVersionedId> toUninstall, Collection<URI> repos, IProgressMonitor monitor) throws ProvisionException {
		Assert.isNotNull(toUninstall);
		IProvisioningAgent agent = getAgent();
		ProvisioningContext ctx = createProvisioningContext(repos, agent);

		//find the ius to uninstall and create the operation
		UninstallOperation resultingOperation = new UninstallOperation(new ProvisioningSession(agent), gatherIUs(listInstalledElements(false, monitor), toUninstall, true, monitor));
		resultingOperation.setProvisioningContext(ctx);
		resultingOperation.setProfileId(IProfileRegistry.SELF);

		return resultingOperation;
	}

	/**
	 * Return the {@link IInstallableUnit} that are installed in the running instance of eclipse.
	 * @param rootsOnly set to true to return only the elements that have been explicitly installed (aka roots).
	 * @param monitor the progress monitor
	 * @return the installable units installed
	 */
	public IQueryResult<IInstallableUnit> listInstalledElements(boolean rootsOnly, IProgressMonitor monitor) {
		IProfileRegistry registry = (IProfileRegistry) getAgent().getService(IProfileRegistry.SERVICE_NAME);
		IProfile profile = registry.getProfile(IProfileRegistry.SELF);
		if (rootsOnly)
			return profile.query(new UserVisibleRootQuery(), monitor);
		return profile.query(QueryUtil.ALL_UNITS, monitor);
	}

	/**
	 * Create an {@link UpdateOperation} that will update the elements specified.
	 * @param toUpdate
	 * @param repos
	 * @param monitor
	 * @return an instance of {@link UpdateOperation}
	 */
	public UpdateOperation createUpdateOperation(Collection<IVersionedId> toUpdate, Collection<URI> repos, IProgressMonitor monitor) throws ProvisionException {
		IProvisioningAgent agent = getAgent();
		ProvisioningContext ctx = createProvisioningContext(repos, agent);

		//find the ius to update and create the operation
		UpdateOperation resultingOperation = new UpdateOperation(new ProvisioningSession(agent), toUpdate == null ? null : gatherIUs(listInstalledElements(false, monitor), toUpdate, false, monitor));
		resultingOperation.setProvisioningContext(ctx);
		resultingOperation.setProfileId(IProfileRegistry.SELF);

		return resultingOperation;
	}

	/**
	 * This factory method creates an {@link SynchronizeOperation} that will cause the current installation to exclusively contain the elements listed once executed.
	 * @param toInstall the elements to install. This can not be null.
	 * @param repos the repositories to install the elements from. 
	 * @param monitor the progress monitor
	 * @return an instance of {@link SynchronizeOperation}.
	 */
	public SynchronizeOperation createSynchronizeOperation(Collection<IVersionedId> toInstall, Collection<URI> repos, IProgressMonitor monitor) throws ProvisionException {
		IProvisioningAgent agent = getAgent();
		ProvisioningContext ctx = createProvisioningContext(repos, agent);

		Collection<IInstallableUnit> iusToInstall;
		if (toInstall == null)
			iusToInstall = ctx.getMetadata(monitor).query(QueryUtil.createIUGroupQuery(), monitor).toUnmodifiableSet();
		else
			iusToInstall = gatherIUs(ctx.getMetadata(monitor), toInstall, false, monitor);

		SynchronizeOperation resultingOperation = new SynchronizeOperation(new ProvisioningSession(agent), iusToInstall);
		resultingOperation.setProvisioningContext(ctx);
		resultingOperation.setProfileId(IProfileRegistry.SELF);

		return resultingOperation;
	}
}
