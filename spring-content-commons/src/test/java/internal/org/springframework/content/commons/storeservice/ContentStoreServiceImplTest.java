package internal.org.springframework.content.commons.storeservice;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.factory.StoreFactory;
import org.springframework.content.commons.storeservice.ContentStoreInfo;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class ContentStoreServiceImplTest {

	private ContentStoreServiceImpl contentRepoService; 
	private StoreFactory mockFactory;
	{
		Describe("ContentStoreServiceImpl", () -> {
			Context("#getContentStores", () -> {
				BeforeEach(() -> {
					contentRepoService = new ContentStoreServiceImpl();
				});
				Context("given no factories", () -> {
					It("should always return empty", () -> {
						assertThat(contentRepoService.getContentStores(), is(new ContentStoreInfo[]{}));
					});
				});
				Context("given a factory", () -> {
					BeforeEach(() -> {
						ContentStore repo = mock(ContentStore.class);
						mockFactory = mock(StoreFactory.class);
						when(mockFactory.getStore()).thenReturn(repo);
						when(mockFactory.getStoreInterface()).thenAnswer(new Answer<Object>() {
							@Override
							public Object answer(InvocationOnMock invocation)
									throws Throwable {
								return ContentRepositoryInterface.class;
							}
						});
						List<StoreFactory> factories = new ArrayList<>();
						factories.add(mockFactory);
						contentRepoService.setFactories(factories);
					});
					It("should return content store information", () -> {
						ContentStoreInfo[] infos = contentRepoService.getContentStores();
						assertThat(infos.length, is(1));
					});
				});
			});
		});
	}
	
	@Test
	public void test() {
	}
	
	public interface ContentRepositoryInterface extends ContentStore<Object, String> {
	}
}
