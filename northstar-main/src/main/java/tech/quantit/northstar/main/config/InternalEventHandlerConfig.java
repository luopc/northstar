package tech.quantit.northstar.main.config;

import java.util.concurrent.ConcurrentMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.InternalEventBus;
import tech.quantit.northstar.domain.account.TradeDayAccount;
import tech.quantit.northstar.domain.gateway.ContractManager;
import tech.quantit.northstar.domain.gateway.GatewayAndConnectionManager;
import tech.quantit.northstar.gateway.sim.trade.SimMarket;
import tech.quantit.northstar.main.factories.TradeDayAccountFactory;
import tech.quantit.northstar.main.handler.internal.AccountHandler;
import tech.quantit.northstar.main.handler.internal.ConnectionHandler;
import tech.quantit.northstar.main.handler.internal.SimMarketHandler;
import tech.quantit.northstar.main.notification.NotificationDispatcher;

@Slf4j
@Configuration
public class InternalEventHandlerConfig {
	
	///////////////////
	/* Internal类事件 */
	///////////////////
	@Bean
	public AccountHandler accountEventHandler(InternalEventBus eventBus, ContractManager contractMgr,
			ConcurrentMap<String, TradeDayAccount> accountMap, GatewayAndConnectionManager gatewayConnMgr) {
		AccountHandler handler = new AccountHandler(accountMap, new TradeDayAccountFactory(gatewayConnMgr, contractMgr));
		log.debug("注册：AccountHandler");
		eventBus.register(handler);
		return handler;
	}
	
	@Bean
	public ConnectionHandler connectionEventHandler(InternalEventBus eventBus, GatewayAndConnectionManager gatewayConnMgr,
			ContractManager contractMgr) {
		ConnectionHandler handler = new ConnectionHandler(gatewayConnMgr, contractMgr);
		log.debug("注册：ConnectionHandler");
		eventBus.register(handler);
		return handler;
	}
	
	@Bean
	public SimMarketHandler simMarketHandler(InternalEventBus eventBus, SimMarket market) {
		SimMarketHandler handler = new SimMarketHandler(market);
		log.debug("注册：SimMarketHandler");
		eventBus.register(handler);
		return handler;
	}
	
	@Bean
	public NotificationDispatcher notificationDispatcher(InternalEventBus eventBus) {
		NotificationDispatcher dispatcher = new NotificationDispatcher();
		log.debug("注册：NotificationDispatcher");
		eventBus.register(dispatcher);
		return dispatcher;
	}
	//////////////////////
	/* Internal类事件结束 */
	//////////////////////
	
}
