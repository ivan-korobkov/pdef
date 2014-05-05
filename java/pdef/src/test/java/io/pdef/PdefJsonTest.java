/*
 * Copyright: 2013 Ivan Korobkov <ivan.korobkov@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.pdef;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.pdef.test.TestException;
import io.pdef.test.TestNumber;
import io.pdef.test.TestStruct;
import static org.fest.assertions.api.Assertions.assertThat;
import org.junit.Test;

import java.util.Date;

public class PdefJsonTest {
	@Test
	public void testStruct() throws Exception {
		TestStruct struct0 = fixtureStruct();
		String json = struct0.toJson();
		TestStruct struct1 = TestStruct.parseJson(json);
		assertThat(struct1).isEqualTo(struct0);
	}

	@Test
	public void testException() throws Exception {
		TestException e = new TestException()
				.setStruct0(fixtureStruct())
				.setMessage("Hello, world");
		String json = e.toJson();
		TestException e1 = TestException.parseJson(json);
		assertThat(e1).isEqualTo(e);
	}

	private TestStruct fixtureStruct() {
		return new TestStruct()
				.setBool0(true)
				.setShort0((short) -16)
				.setInt0(-32)
				.setLong0(-64)
				.setFloat0(-1.5f)
				.setDouble0(-2.5f)
				.setString0("Привет")
				.setDatetime0(new Date(0))
				.setList0(ImmutableList.of(1, 2, 3))
				.setSet0(ImmutableSet.of(4, 5, 6))
				.setMap0(ImmutableMap.of(1, "a", 2, "b"))
				.setEnum0(TestNumber.ONE)
				.setStruct0(new TestStruct());
	}
}
