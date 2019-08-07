package eu.xword.nixer.nixerplugin.login.jdbc;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.DatabasePopulator;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

@Component
public class JdbcDAOConfigurer {

    private DataSource dataSource;

    private List<Resource> initScripts = new ArrayList<>();

    public JdbcDAOConfigurer(final DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void postInit() {
        this.initScripts.add(new ClassPathResource(
                "eu/xword/nixer/nixerplugin/login/jdbc/schema.ddl"));
        getDataSourceInit().afterPropertiesSet();
    }

    protected DatabasePopulator getDatabasePopulator() {
        ResourceDatabasePopulator dbp = new ResourceDatabasePopulator();
        dbp.setScripts(initScripts.toArray(new Resource[initScripts.size()]));
        return dbp;
    }

    private DataSourceInitializer getDataSourceInit() {
        DataSourceInitializer dsi = new DataSourceInitializer();
        dsi.setDatabasePopulator(getDatabasePopulator());
        dsi.setDataSource(dataSource);
        return dsi;
    }
}
