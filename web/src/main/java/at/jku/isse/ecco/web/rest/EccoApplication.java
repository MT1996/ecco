package at.jku.isse.ecco.web.rest;

import at.jku.isse.ecco.service.EccoService;
import org.glassfish.jersey.server.ResourceConfig;
import java.nio.file.Paths;

/*
ApplicationPath hat leider keine Auswirkung auf die URI auf der die API laufen wird
siehe https://github.com/eclipse-ee4j/jersey/issues/4205

Issue ist bisher auch nicht closed hat auch keinen Workaround...

 */
//@ApplicationPath("ecco")
public class EccoApplication extends ResourceConfig {

	private EccoService eccoService = new EccoService();

	public EccoApplication() {
		packages("at.jku.isse.ecco.web.rest");
//
//		property("eccoService", this.eccoService);
//
//		register(CorsFilter.class);
	}

	public EccoService getEccoService() {
		return this.eccoService;
	}

	public void init(String repositoryDir) {
		System.out.println(String.format("Repository Direction set to: %s", Paths.get(repositoryDir)));

		this.eccoService.setRepositoryDir(Paths.get(repositoryDir));
		this.eccoService.open();
	}

	public void destroy() {
		this.eccoService.close();
	}

}
