package cn.wildfirechat.app;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;

import static org.junit.Assert.assertNotNull;

/**
 * Application Tests
 *
 * 数据库配置说明：
 * - 默认使用 H2 内存数据库（无需外部数据库即可运行测试）
 * - 如需测试 MySQL，启用 MySqlTestConfig
 * - 如需测试达梦数据库，启用 DamengTestConfig
 *
 * 启用方式：将对应配置类的 @Primary 注解取消注释即可
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ApplicationTests {

	@Test
	public void contextLoads() {
		assertNotNull("Application should load", true);
	}

	// ==================== H2 内存数据库配置（默认） ====================
	@TestConfiguration
	static class H2TestConfig {
		@Bean
		@Primary
		public DataSource dataSource() {
			HikariDataSource dataSource = new HikariDataSource();
			dataSource.setDriverClassName("org.h2.Driver");
			dataSource.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL");
			dataSource.setUsername("sa");
			dataSource.setPassword("");
			dataSource.setMinimumIdle(1);
			dataSource.setMaximumPoolSize(5);
			return dataSource;
		}
	}

	// ==================== MySQL 数据库配置示例 ====================
	// 使用方法：取消下方 @Primary 注解的注释，并将 H2TestConfig 的 @Primary 注释掉
/*
	@TestConfiguration
	static class MySqlTestConfig {
		@Bean
		@Primary
		public DataSource dataSource() {
			HikariDataSource dataSource = new HikariDataSource();
			dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
			dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/appdata?serverTimezone=UTC&useSSL=false");
			dataSource.setUsername("root");
			dataSource.setPassword("123456");
			dataSource.setMinimumIdle(1);
			dataSource.setMaximumPoolSize(10);
			return dataSource;
		}
	}
*/

	// ==================== 达梦数据库配置示例 ====================
	// 使用方法：取消下方 @Primary 注解的注释，并将 H2TestConfig 的 @Primary 注释掉
/*
	@TestConfiguration
	static class DamengTestConfig {
		@Bean
		@Primary
		public DataSource dataSource() {
			HikariDataSource dataSource = new HikariDataSource();
			dataSource.setDriverClassName("dm.jdbc.driver.DmDriver");
			dataSource.setJdbcUrl("jdbc:dm://192.168.1.6:5237");
			dataSource.setUsername("SYSDBA");
			dataSource.setPassword("Wfc123!@");
			dataSource.setMinimumIdle(1);
			dataSource.setMaximumPoolSize(5);
			return dataSource;
		}
	}
*/

}
