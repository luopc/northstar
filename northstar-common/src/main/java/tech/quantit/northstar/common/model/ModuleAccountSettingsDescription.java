package tech.quantit.northstar.common.model;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模组账户配置信息
 * @author KevinHuangwl
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModuleAccountSettingsDescription {

	/**
	 * 账户网关ID
	 */
	private String accountGatewayId;
	/**
	 * 模组账户初始金额
	 */
	private int moduleAccountInitBalance;
	/**
	 * 账户关联合约名称
	 */
	private Set<String> bindedUnifiedSymbol;
}
