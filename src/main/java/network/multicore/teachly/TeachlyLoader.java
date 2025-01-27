package network.multicore.teachly;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

public class TeachlyLoader implements PluginLoader {

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();


        resolver.addRepository(new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build());

        String version = "24.1.2";

        resolver.addDependency(new Dependency(new DefaultArtifact("org.graalvm.polyglot", "polyglot", "jar", version), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("org.graalvm.polyglot", "js", "pom", version), null));

        classpathBuilder.addLibrary(resolver);
    }
}
