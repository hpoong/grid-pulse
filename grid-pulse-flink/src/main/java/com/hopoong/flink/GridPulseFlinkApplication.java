package com.hopoong.flink;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class GridPulseFlinkApplication {
	public static void main(String[] args) throws Exception {
		// 1) 실행 환경 생성
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		// 2) 간단한 데이터 소스 (메모리 안의 값들)
		DataStream<String> source = env.fromElements(
				"my flink test",
				"hello flink",
				"flink java stream"
		);

		// 3) 대문자로 변환
		DataStream<String> upper = source
				.map(value -> value.toUpperCase());

		// 4) 결과 출력
		upper.print();

		// 5) Job 실행
		env.execute("My First Flink Job v4");
	}
}
