package tech.xuanwu.northstar.strategy.cta.module.signal;

import tech.xuanwu.northstar.strategy.common.CtaSignalPolicy;
import tech.xuanwu.northstar.strategy.common.annotation.Label;
import tech.xuanwu.northstar.strategy.common.annotation.StrategicComponent;
import tech.xuanwu.northstar.strategy.common.model.BarData;
import tech.xuanwu.northstar.strategy.common.model.DynamicParams;
import tech.xuanwu.northstar.strategy.common.model.CtaSignal;
import xyz.redtorch.pb.CoreField.TickField;

@StrategicComponent("示例策略")
public class SampleSignalPolicy implements CtaSignalPolicy{
	
	/**
	 * 绑定合约
	 */
	private String bindedUnifiedSymbol;

	
	@Override
	public CtaSignal updateTick(TickField tick, BarData barData) {
		
		return null;
	}

	@Override
	public DynamicParams getDynamicParams() {
		return new InitParams();
	}

	@Override
	public void initWithParams(DynamicParams params) {
		InitParams initParams = (InitParams) params;
		bindedUnifiedSymbol = initParams.unifiedSymbol;
	}
	
	public class InitParams extends DynamicParams{
		
		@Label(value="绑定合约", order=1)
		private String unifiedSymbol;
		
	}
}