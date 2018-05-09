package cz.it4i.parallel;

import static org.mockito.Mockito.doAnswer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.scijava.Context;
import org.scijava.command.Command;
import org.scijava.plugin.SciJavaPlugin;

import com.google.common.base.Function;

import net.imagej.Dataset;

public class ImageJServerWorker implements ParallelWorker {

	private final String hostName;
	private final int port;
	private final Map<Dataset,String> mockedData2id = new HashMap<>();
	private final Map<String, Dataset> id2mockedData = new HashMap<>();
	
	private final static Set<String> supportedImageTypes = Collections
			.unmodifiableSet(new HashSet<>(Arrays.asList("png", "jpg")));

	ImageJServerWorker(String hostName, int port) {
		this.hostName = hostName;
		this.port = port;
	}

	public String getHostName() {
		return hostName;
	}

	public int getPort() {
		return port;
	}
	
	// -- ParallelWorker methods --

	@Override
	public Dataset importData(Path path) {
		
		final String filePath = path.toAbsolutePath().toString();
		final String fileName = path.getFileName().toString();
				
		Dataset result = null;

		try {
			
			HttpEntity entity = MultipartEntityBuilder.create()
					.addBinaryBody("file", new File(filePath), ContentType.create(getContentType(filePath)), fileName).build();
			
			HttpPost httpPost = new HttpPost("http://" + hostName + ":" + String.valueOf(port) + "/objects/upload");
			httpPost.setEntity(entity);
			
			HttpClient httpClient = HttpClientBuilder.create().build();			
			
			HttpResponse response = httpClient.execute(httpPost);
			
			// TODO check result code properly
			
			String json = EntityUtils.toString(response.getEntity());			
			String obj = new org.json.JSONObject(json).getString("id");
			
			result = Mockito.mock(Dataset.class, (Answer<Dataset>) p -> {
				throw new UnsupportedOperationException();
			});
			doAnswer(p -> "Dataset(mocked)[id = " + obj).when(result).toString();
			
			mockedData2id.put(result, obj);
			id2mockedData.put(obj, result);
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	@Override
	public void exportData(Dataset dataset, Path p) {
		String filePath = p.toString();
		String id = mockedData2id.get(dataset);
		String getUrl = "http://" + hostName + ":" + String.valueOf(port) + "/objects/" + id + "/" + getImageType(filePath);
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet(getUrl);

		try {

			HttpResponse response = httpClient.execute(get);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				// long len = entity.getContentLength();
				BufferedInputStream bis = new BufferedInputStream(entity.getContent());
				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(filePath)));
				int inByte;
				while ((inByte = bis.read()) != -1) {
					bos.write(inByte);
				}
				bis.close();
				bos.close();

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void deleteData(Dataset ds) {
		String id = mockedData2id.get(ds);
		
		@SuppressWarnings("unused")
		String json = null;
	
		String postUrl = "http://" + hostName + ":" + String.valueOf(port) + "/objects/" + id;
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpDelete delete = new HttpDelete(postUrl);
	
		try {
	
			HttpResponse response = httpClient.execute(delete);
			json = EntityUtils.toString(response.getEntity());
			//TODO check result code
		} catch (Exception e) {
			e.printStackTrace();
		}
	
		mockedData2id.remove(ds);
		id2mockedData.remove(id);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Command> Map<String, Object> executeCommand(Class<T> commandType, Map<String, ?> inputs) {
		Map<String,Object> map = wrapInputValues(inputs);
		String json = null;

		String postUrl = "http://" + hostName + ":" + String.valueOf(port) + "/modules/" + "command:"
				+ commandType.getCanonicalName();
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(postUrl);

		try {

			JSONObject reqJson = new JSONObject();

			for (Map.Entry<String, ?> pair : map.entrySet()) {
				reqJson.put(pair.getKey(), pair.getValue());
			}

			StringEntity postingString = new StringEntity(reqJson.toString());
			post.setEntity(postingString);
			post.setHeader("Content-type", "application/json");
			HttpResponse response = httpClient.execute(post);

			json = EntityUtils.toString(response.getEntity());

		} catch (Exception e) {
			e.printStackTrace();
		}

		Map<String,Object> result = new HashMap<>();
		org.json.JSONObject jsonObj = new org.json.JSONObject(json);
		for(String key: jsonObj.keySet()) {
			result.put(key, jsonObj.get(key));
		}
		return unwrapOutputValues(result);
	}
	
	// -- Helper methods --

	// TODO: support another types
	private String getContentType(String path) {
		return "image/" + getImageType(path);
	}
	
	private String getImageType(String path) {
		for (String type : supportedImageTypes) {
			if (path.endsWith("." + type)) {
				return type;
			}
		}

		throw new UnsupportedOperationException("Only " + supportedImageTypes + " image files supported");
	}

	private Map<String, Object> wrapInputValues(Map<String, ?> map) {
		return convertMap(map, this::isEntryResolvable, this::wrapValue);
	}
	
	private Map<String, Object> unwrapOutputValues(Map<String, Object> map) {
		return convertMap(map, this::isEntryResolvable, this::unwrapValue);
	}

	/**
	 * Converts an input map into an output map
	 * @param map - an input map
	 * @param filter - a filter to be applied on all map entries prior the actual conversion
	 * @param converter - a converter to be applied on each map entry
	 * @return a converted map
	 */
	private Map<String, Object> convertMap(Map<String, ?> map, Function<Map.Entry<String, ?>, Boolean> filter, Function<Object, Object> converter) {
		return map.entrySet().stream()
				.filter(entry -> filter.apply(entry))
				.map(entry -> new SimpleImmutableEntry<String, Object>(entry.getKey(), converter.apply(entry.getValue())))
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
	}
	
	// TODO: Should not we return null if it is not instance of any supported type? 
	private Object wrapValue(Object value) {
		if (value instanceof Dataset) {
			Dataset ds = (Dataset) value;
			Object id = mockedData2id.get(ds);
			if (id != null) {
				value = id;
			}
		}
		return value;
	}
	
	// TODO: Should not we return null if it is not instance of any supported type? 
	private Object unwrapValue(Object value) {
		Dataset obj = id2mockedData.get(value);
		if (obj != null) {
			value = obj;
		}
		return value;
	}
	
	/**
	 * Determines whether an entry is resolvable from the SciJava Context
	 */
	private Boolean isEntryResolvable(Map.Entry<String, ?> entry) {
		return entry.getValue() != null && !(entry.getValue() instanceof SciJavaPlugin)
				&& !(entry.getValue() instanceof Context);
	}
}
