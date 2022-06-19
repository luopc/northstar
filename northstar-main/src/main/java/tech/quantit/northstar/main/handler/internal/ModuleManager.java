package tech.quantit.northstar.main.handler.internal;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import tech.quantit.northstar.common.event.AbstractEventHandler;
import tech.quantit.northstar.common.event.NorthstarEvent;
import tech.quantit.northstar.common.event.NorthstarEventType;
import tech.quantit.northstar.strategy.api.IModule;

@Slf4j
public class ModuleManager extends AbstractEventHandler{
	/**
	 * moduleName --> module
	 */
	protected ConcurrentHashMap<String, IModule> moduleMap = new ConcurrentHashMap<>(50);
	
	private Set<NorthstarEventType> eventSet = new HashSet<>();
	
	public ModuleManager() {
		eventSet.add(NorthstarEventType.ACCOUNT);
		eventSet.add(NorthstarEventType.TRADE);
		eventSet.add(NorthstarEventType.ORDER);
		eventSet.add(NorthstarEventType.TICK);
		eventSet.add(NorthstarEventType.BAR);
		eventSet.add(NorthstarEventType.EXT_MSG);
	}
	
	public void addModule(IModule module) {
		moduleMap.put(module.getName(), module);
	}
	
	public void removeModule(String name) {
		IModule module = moduleMap.get(name);
		if(module == null) {
			log.debug("[{}] 已删除", name);
			return;
		} 	
		if(module.isEnabled()) {
			throw new IllegalStateException("模组处于启用状态，不允许移除");
		}
		moduleMap.remove(name);
	}
	
	public IModule getModule(String name) {
		if(!moduleMap.containsKey(name)) {
			throw new IllegalStateException("没有找到模组：" + name);
		}
		return moduleMap.get(name);
	}
	

	@Override
	public boolean canHandle(NorthstarEventType eventType) {
		return eventSet.contains(eventType);
	}

	@Override
	protected void doHandle(NorthstarEvent e) {
		moduleMap.values().parallelStream().forEach(sm -> sm.onEvent(e));
	}

}