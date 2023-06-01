package org.dromara.northstar.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import xyz.redtorch.pb.CoreEnum.ExchangeEnum;

class ContractUtilsTest {

	@Test
	void testGetMonthlyUnifiedSymbolOfIndexContract() {
		assertThat(ContractUtils.getMonthlyUnifiedSymbolOfIndexContract("rb0000@SHFE@FUTURES", ExchangeEnum.SHFE)).hasSize(13);
		assertThat(ContractUtils.getMonthlyUnifiedSymbolOfIndexContract("AP0000@SHFE@FUTURES", ExchangeEnum.CZCE)).hasSize(13);
	}

}
