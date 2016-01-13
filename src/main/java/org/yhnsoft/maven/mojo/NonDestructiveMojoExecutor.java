package org.yhnsoft.maven.mojo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.CumulativeScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.lifecycle.internal.DependencyContext;
import org.apache.maven.lifecycle.internal.LifecycleDependencyResolver;
import org.apache.maven.lifecycle.internal.MojoExecutor;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;

@Component(role = MojoExecutor.class)
public class NonDestructiveMojoExecutor extends MojoExecutor {
	@Requirement
	private LifecycleDependencyResolver lifeCycleDependencyResolver;
	
	@Override
	public void ensureDependenciesAreResolved(MojoDescriptor mojoDescriptor, MavenSession session, DependencyContext dependencyContext) throws LifecycleExecutionException {
		MavenProject project = dependencyContext.getProject();
		boolean aggregating = mojoDescriptor.isAggregator();
		
		if (dependencyContext.isResolutionRequiredForCurrentProject()) {
			Collection<String> scopesToCollect = dependencyContext.getScopesToCollectForCurrentProject();
			Collection<String> scopesToResolve = dependencyContext.getScopesToResolveForCurrentProject();
			
			lifeCycleDependencyResolver.resolveProjectDependencies(project, scopesToCollect, scopesToResolve, session,
				aggregating, Collections.<Artifact>emptySet());
			
			dependencyContext.synchronizeWithProjectState();
		}
		
		if (aggregating) {
			Collection<String> scopesToCollect = toScopes(mojoDescriptor.getDependencyCollectionRequired());
			Collection<String> scopesToResolve = toScopes(mojoDescriptor.getDependencyResolutionRequired());
			
			if (dependencyContext.isResolutionRequiredForAggregatedProjects(scopesToCollect, scopesToResolve)) {
				for (MavenProject aggregatedProject : collectChildModules(project, session.getProjects())) {
					lifeCycleDependencyResolver.resolveProjectDependencies(aggregatedProject, scopesToCollect,
						scopesToResolve, session, true,
						Collections.<Artifact>emptySet());
				}
			}
		}
		
		ArtifactFilter artifactFilter = getArtifactFilter(mojoDescriptor);
		List<MavenProject> projectsToResolve =
			LifecycleDependencyResolver.getProjects(session.getCurrentProject(), session,
				mojoDescriptor.isAggregator());
		for (MavenProject projectToResolve : projectsToResolve) {
			projectToResolve.setArtifactFilter(artifactFilter);
		}
	}
	
	private List<MavenProject> collectChildModules(MavenProject parent, List<MavenProject> projects) {
		List<MavenProject> children = new ArrayList<>();
		
		for (MavenProject project : projects) {
			if (project.getParent() == parent) {
				children.add(project);
				children.addAll(collectChildModules(project, projects));
			}
		}
		
		return children;
	}
	
	private Collection<String> toScopes(String classpath) {
		if (StringUtils.isNotEmpty(classpath)) {
			if (Artifact.SCOPE_COMPILE.equals(classpath)) {
				return Arrays.asList(Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_PROVIDED);
			}
			else if (Artifact.SCOPE_RUNTIME.equals(classpath)) {
				return Arrays.asList(Artifact.SCOPE_COMPILE, Artifact.SCOPE_RUNTIME);
			}
			else if (Artifact.SCOPE_COMPILE_PLUS_RUNTIME.equals(classpath)) {
				return Arrays.asList(Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_PROVIDED,
					Artifact.SCOPE_RUNTIME);
			}
			else if (Artifact.SCOPE_RUNTIME_PLUS_SYSTEM.equals(classpath)) {
				return Arrays.asList(Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_RUNTIME);
			}
			else if (Artifact.SCOPE_TEST.equals(classpath)) {
				return Arrays.asList(Artifact.SCOPE_COMPILE, Artifact.SCOPE_SYSTEM, Artifact.SCOPE_PROVIDED,
					Artifact.SCOPE_RUNTIME, Artifact.SCOPE_TEST);
			}
		}
		return Collections.emptyList();
	}
	
	private ArtifactFilter getArtifactFilter(MojoDescriptor mojoDescriptor) {
		String scopeToResolve = mojoDescriptor.getDependencyResolutionRequired();
		String scopeToCollect = mojoDescriptor.getDependencyCollectionRequired();
		
		List<String> scopes = new ArrayList<>(2);
		if (StringUtils.isNotEmpty(scopeToCollect)) {
			scopes.add(scopeToCollect);
		}
		if (StringUtils.isNotEmpty(scopeToResolve)) {
			scopes.add(scopeToResolve);
		}
		
		if (scopes.isEmpty()) {
			return null;
		}
		else {
			return new CumulativeScopeArtifactFilter(scopes);
		}
	}
}
