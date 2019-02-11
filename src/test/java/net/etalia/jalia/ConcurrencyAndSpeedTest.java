package net.etalia.jalia;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.etalia.jalia.DummyAddress.AddressType;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("performance")
public class ConcurrencyAndSpeedTest {

	private long sleep = 0;
	
	private ObjectMapper mapper = new ObjectMapper();

	private Map<String,Object> map = new HashMap<>();
	private byte[] mapBytes;
	private DummyPerson person = null;
	private byte[] personFullBytes;
	private byte[] personFieldsBytes;
	private List<Object> list = new ArrayList<>();
	private byte[] listBytes;
	
	private volatile boolean hasErrors = false;
	private ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
	
	@Before
	public void setup() {
		DummyEntityProvider provider = new DummyEntityProvider();
		mapper.setClassDataFactory(provider);
		mapper.setEntityFactory(provider);
		mapper.setEntityNameProvider(provider);
		
		map.put("testInt", 1);
		map.put("testString", "string");
		map.put("testNull", null);
		Map<String,Object> sub = new HashMap<>();
		sub.put("subString","string");
		map.put("testSub", sub);
		mapBytes = mapper.writeValueAsBytes(map);
		
		person = new DummyPerson("p1", "Simone", "Gianni", new DummyAddress("a1", AddressType.EMAIL, "simoneg@apache.org"));
		personFullBytes = mapper.writeValueAsBytes(person);
		OutField fields = new OutField(null);
		fields.getCreateSubs("name","tags","addresses.type");
		personFieldsBytes = mapper.writeValueAsBytes(person, fields);
		
		list.add(1);
		list.add("string");
		list.add(null);
		List<Object> subList = new ArrayList<>();
		subList.add("string");
		list.add(subList);
		listBytes = mapper.writeValueAsBytes(list);
	}
	
	
	private class Tester implements Runnable {

		private Object obj;
		private OutField fields;
		private byte[] compare;
		
		public Tester(Object obj, OutField fields, byte[] compare) {
			super();
			this.obj = obj;
			this.fields = fields;
			this.compare = compare;
		}

		@Override
		public void run() {
			try {
				byte[] res = mapper.writeValueAsBytes(obj, fields);
				if (!Arrays.equals(res, compare)) throw new IllegalStateException("Result is different");
			} catch (Throwable e) {
				hasErrors = true;
				errors.add(e);
			}
		}
		
	}
	
	@Test
	public void concurrency() throws Exception {
		ExecutorService exec = Executors.newFixedThreadPool(10);
		
		OutField fields = new OutField(null);
		fields.getCreateSubs("name","tags","addresses.type");
		// Schedule tasks
		for (int i = 0; i < 10000; i++) {
			exec.submit(new Tester(map, null, mapBytes));
			exec.submit(new Tester(list, null, listBytes));
			exec.submit(new Tester(person, null, personFullBytes));
			exec.submit(new Tester(person, fields, personFieldsBytes));
		}
		
		exec.shutdown();
		exec.awaitTermination(10, TimeUnit.SECONDS);
		
		System.out.println(errors);
		assertThat(hasErrors, equalTo(false));
	}

	@Test
	public void speedOnSerializingFullBean() throws Exception {
		final int rounds = 1_000_000;
		
		long tot = 0;
		
		// Ramp up
		for (int i = 0; i < rounds/10; i++) {
			tot += mapper.writeValueAsBytes(person,null).length;
		}
		
		tot = 0;
		if (sleep > 0) {
			System.out.println("About to start test, hook up profiler");
			Thread.sleep(sleep);
			System.out.println("Test started");
		}
		
		long start = System.currentTimeMillis();
		for (int i = 0; i < rounds; i++) {
			tot += mapper.writeValueAsBytes(person,null).length;
		}
		long elaps = System.currentTimeMillis() - start;
		System.out.println(rounds + "\t Full bean ser done \t in " + elaps + "ms \t = " + (elaps / (double)rounds) + "ms each \t (tot bytes " + tot + ")");
	}

	@Test
	public void speedOnSerializingBeanWithFields() throws Exception {
		final int rounds = 1_000_000;

		long tot = 0;
		
		OutField fields = new OutField(null);
		fields.getCreateSubs("name","tags","addresses.type");
		
		// Ramp up
		for (int i = 0; i < rounds/10; i++) {
			tot += mapper.writeValueAsBytes(person,fields).length;
		}
		
		tot = 0;
		if (sleep > 0) {
			System.out.println("About to start test, hook up profiler");
			Thread.sleep(sleep);
			System.out.println("Test started");
		}
		
		long start = System.currentTimeMillis();
		for (int i = 0; i < rounds; i++) {
			tot += mapper.writeValueAsBytes(person,fields).length;
		}
		long elaps = System.currentTimeMillis() - start;
		System.out.println(rounds + "\t Beans fields ser done \t in " + elaps + "ms \t = " + (elaps / (double)rounds) + "ms each \t (tot bytes " + tot + ")");
	}
	
	@Test
	public void speedOnDeserializingFullBean() throws Exception {
		final int rounds = 1_000_000;
		
		long tot = 0;
		
		// Ramp up
		for (int i = 0; i < rounds/10; i++) {
			tot ^= mapper.readValue(personFullBytes, DummyPerson.class).hashCode();
		}
		
		tot = 0;
		if (sleep > 0) {
			System.out.println("About to start test, hook up profiler");
			Thread.sleep(sleep);
			System.out.println("Test started");
		}
		
		long start = System.currentTimeMillis();
		DummyPerson p = null;
		for (int i = 0; i < rounds; i++) {
			p = mapper.readValue(personFullBytes, DummyPerson.class);
			tot ^= p.hashCode();
		}
		long elaps = System.currentTimeMillis() - start;
		System.out.println(rounds + "\t Full bean deser (CR) done \t in " + elaps + "ms \t = " + (elaps / (double)rounds) + "ms each");
		System.out.println(p);
	}

	@Test
	public void speedOnDeserializingFullBeanNoRecording() throws Exception {
		final int rounds = 1_000_000;

		long tot = 0;
		mapper.setOption(DefaultOptions.RECORD_CHANGES, false);

		// Ramp up
		for (int i = 0; i < rounds/10; i++) {
			tot ^= mapper.readValue(personFullBytes, DummyPerson.class).hashCode();
		}

		tot = 0;
		if (sleep > 0) {
			System.out.println("About to start test, hook up profiler");
			Thread.sleep(sleep);
			System.out.println("Test started");
		}

		long start = System.currentTimeMillis();
		DummyPerson p = null;
		for (int i = 0; i < rounds; i++) {
			p = mapper.readValue(personFullBytes, DummyPerson.class);
			tot ^= p.hashCode();
		}
		long elaps = System.currentTimeMillis() - start;
		System.out.println(rounds + "\t Full bean deser (no CR) done \t in " + elaps + "ms \t = " + (elaps / (double)rounds) + "ms each");
		System.out.println(p);
	}
	
	@Test
	public void concurrentVersusLocked() throws Exception {
		
	}
}
