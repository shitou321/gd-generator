package io.gd.generator.handler;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import freemarker.template.Version;
import io.gd.generator.Config;
import io.gd.generator.GenLog;
import io.gd.generator.util.ClassHelper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractHandler implements Handler {

	Logger logger = LoggerFactory.getLogger(AbstractHandler.class);

	protected Config config;

	protected Configuration freemarkerConfiguration;

	protected GenLog genLog;

	@Override
	public void handle(Config config) throws Exception {
		Objects.requireNonNull(config, " config cat not be null");
		this.config = config;
		try {
			init();
			doHandle();
		} catch (Exception e) {
			logger.error("generate error", e);
		} finally {
			try {
				destroy();
			} catch (Exception e) {
				logger.error("destroy error", e);
			}
		}
	}

	protected void init() throws Exception {
		freemarkerConfiguration = new Configuration(new Version(config.getFreemakerVersion()));
		freemarkerConfiguration.setDefaultEncoding(config.getDefaultEncoding());
		freemarkerConfiguration.setClassForTemplateLoading(getClass(), "/" + config.getTemplate());
		genLog = new GenLog(config.getGenLogFile());
	}

	protected void destroy() throws Exception {
		genLog.flush();
	}

	protected void doHandle() {
		/* 获取所有 entity */
		Set<Class<?>> entityClasses = ClassHelper.getClasses(config.getEntityPackage());
		/* 遍历生成 */
		entityClasses.stream().forEach(entityClass -> {
			if (entityClass.getDeclaredAnnotation(Entity.class) != null) {
				try {
					/* 顺次生成每一个 */
					doHandleOne(entityClass);
					logger.info("generate " + entityClass.getName() + " success");
				} catch (Exception e) {
					logger.error("generate " + entityClass.getName() + " error", e);
				}
			} else {
				logger.info("generate " + entityClass.getName() + " skipped");
			}
		});

	}

	protected void doHandleOne(Class<?> entityClass) throws Exception {

	};
	
	protected String renderTemplate(String tmplName, Map<String, Object> model) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		StringWriter out = new StringWriter();
		Template template = freemarkerConfiguration.getTemplate(tmplName + ".ftl");
		template.process(model, out);
		return out.toString();
	}

}
