/**
 * Copyright 2014-2016 yangming.liu<bytefox@126.com>.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, see <http://www.gnu.org/licenses/>.
 */
package org.bytesoft.bytejta.supports.dubbo;

import java.util.ArrayList;
import java.util.List;

import org.bytesoft.common.utils.CommonUtils;
import org.bytesoft.transaction.TransactionBeanFactory;
import org.bytesoft.transaction.aware.TransactionBeanFactoryAware;
import org.bytesoft.transaction.aware.TransactionEndpointAware;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ProtocolConfig;

public class TransactionEndpointPostProcessor implements BeanFactoryPostProcessor, TransactionBeanFactoryAware {
	private TransactionBeanFactory beanFactory;

	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();

		BeanDefinition applicationDef = null;
		BeanDefinition protocolDef = null;

		List<BeanDefinition> beanDefList = new ArrayList<BeanDefinition>();
		String[] beanNameArray = beanFactory.getBeanDefinitionNames();
		for (int i = 0; i < beanNameArray.length; i++) {
			String beanName = beanNameArray[i];
			BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);
			String beanClassName = beanDef.getBeanClassName();

			Class<?> beanClass = null;
			try {
				beanClass = cl.loadClass(beanClassName);
			} catch (Exception ex) {
				continue;
			}

			if (TransactionEndpointAware.class.isAssignableFrom(beanClass)) {
				beanDefList.add(beanDef);
			} else if (ProtocolConfig.class.isAssignableFrom(beanClass)) {
				if (protocolDef == null) {
					protocolDef = beanDef;
				} else {
					throw new FatalBeanException("There are more than one com.alibaba.dubbo.config.ProtocolConfig was found!");
				}
			} else if (ApplicationConfig.class.isAssignableFrom(beanClass)) {
				if (applicationDef == null) {
					applicationDef = beanDef;
				} else {
					throw new FatalBeanException(
							"There are more than one com.alibaba.dubbo.config.ApplicationConfig was found!");
				}
			}
		}

		if (applicationDef == null) {
			throw new FatalBeanException("No configuration of class com.alibaba.dubbo.config.ApplicationConfig was found.");
		} else if (protocolDef == null) {
			throw new FatalBeanException("No configuration of class com.alibaba.dubbo.config.ProtocolConfig was found.");
		}

		MutablePropertyValues applicationValues = applicationDef.getPropertyValues();
		PropertyValue applicationValue = applicationValues.getPropertyValue("name");
		if (applicationValue == null || applicationValue.getValue() == null) {
			throw new FatalBeanException("Attribute 'name' of <dubbo:application ... /> is null.");
		}

		MutablePropertyValues protocolValues = protocolDef.getPropertyValues();
		PropertyValue protocolValue = protocolValues.getPropertyValue("port");
		if (protocolValue == null || protocolValue.getValue() == null) {
			throw new FatalBeanException("Attribute 'port' of <dubbo:protocol ... /> is null.");
		}

		String host = CommonUtils.getInetAddress();
		String name = String.valueOf(applicationValue.getValue());
		String port = String.valueOf(protocolValue.getValue());
		String identifier = String.format("%s:%s:%s", host, name, port);

		for (int i = 0; i < beanDefList.size(); i++) {
			BeanDefinition beanDef = beanDefList.get(i);
			MutablePropertyValues mpv = beanDef.getPropertyValues();
			mpv.addPropertyValue(TransactionEndpointAware.ENDPOINT_FIELD_NAME, identifier);
		}

	}

	public TransactionBeanFactory getBeanFactory() {
		return beanFactory;
	}

	public void setBeanFactory(TransactionBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

}
