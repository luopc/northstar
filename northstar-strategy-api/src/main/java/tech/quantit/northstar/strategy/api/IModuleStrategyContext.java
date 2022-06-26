package tech.quantit.northstar.strategy.api;

import java.util.function.Function;

import org.slf4j.Logger;

import tech.quantit.northstar.common.constant.ModuleState;
import tech.quantit.northstar.common.constant.SignalOperation;
import tech.quantit.northstar.common.model.TimeSeriesValue;
import tech.quantit.northstar.strategy.api.constant.PriceType;
import tech.quantit.northstar.strategy.api.indicator.Indicator;
import tech.quantit.northstar.strategy.api.indicator.Indicator.ValueType;
import xyz.redtorch.pb.CoreField.ContractField;

public interface IModuleStrategyContext {
	/**
	 * 获取模组名称
	 * @return
	 */
	String getModuleName();
	/**
	 * 获取合约
	 * @param unifiedSymbol
	 * @return
	 */
	ContractField getContract(String unifiedSymbol);
	/**
	 * 委托下单（精简接口）
	 * @param gatewayId
	 * @param contract
	 * @param operation
	 * @param priceType
	 * @param volume
	 * @param price
	 * @return
	 */
	String submitOrderReq(ContractField contract, SignalOperation operation, PriceType priceType, int volume, double price);
	/**
	 * 撤单
	 * @param cancelReq
	 */
	void cancelOrder(String originOrderId);
	/**
	 * 获取模组状态
	 * @return
	 */
	ModuleState getState();
	/**
	 * 停用模组策略
	 * @param enabled
	 */
	void disabledModule();
	/**
	 * 获取日志对象
	 * @return
	 */
	Logger getLogger();
	/**
	 * 创建指标
	 * @param indicatorName
	 * @param bindedUnifiedSymbol
	 * @param indicatorLength
	 * @param valTypeOfBar
	 * @param updateValPublisher
	 * @return
	 */
	Indicator newIndicator(String indicatorName, String bindedUnifiedSymbol, int indicatorLength, ValueType valTypeOfBar,
			Function<TimeSeriesValue, TimeSeriesValue> indicatorFunction);
	/**
	 * 创建指标（采用默认长度与收盘价取值）
	 * @param indicatorName
	 * @param bindedUnifiedSymbol
	 * @param indicatorLength
	 * @param valTypeOfBar
	 * @param updateValPublisher
	 * @return
	 */
	Indicator newIndicator(String indicatorName, String bindedUnifiedSymbol, Function<TimeSeriesValue, TimeSeriesValue> indicatorFunction);
}
