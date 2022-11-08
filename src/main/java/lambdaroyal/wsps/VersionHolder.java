package lambdaroyal.wsps;


import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;
@Repository
public class VersionHolder {
	
	private String version;
    public VersionHolder(ApplicationContext context) {
        String version = context.getBean(Main.class).getClass().getPackage().getImplementationVersion();
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}