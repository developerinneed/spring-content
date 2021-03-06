package internal.org.springframework.content.fs.repository;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.utils.FileService;
import org.springframework.content.fs.io.DeletableResource;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class DefaultFilesystemStoresImplTest {
    private DefaultFilesystemStoreImpl<TestEntity, String> filesystemContentRepoImpl;
    private FileSystemResourceLoader loader;
    private ConversionService conversion;
    private TestEntity entity;
    
    private Resource resource;
    private WritableResource writeableResource;
    private DeletableResource deletableResource;
    private DeletableResource nonExistentResource;
    private FileService fileService;

    private InputStream content;
    private OutputStream output;

    private File parent;

    private InputStream result;

    {
        Describe("DefaultFilesystemContentRepositoryImpl", () -> {
            BeforeEach(() -> {
                writeableResource = mock(WritableResource.class);
                loader = mock(FileSystemResourceLoader.class);
                conversion= mock(ConversionService.class);

                fileService = mock(FileService.class);

                filesystemContentRepoImpl = new DefaultFilesystemStoreImpl<TestEntity, String>(loader, conversion, fileService);
            });
            
            Context("#setContent", () -> {
                BeforeEach(() -> {
                    entity = new TestEntity();
                    content = new ByteArrayInputStream("Hello content world!".getBytes());

                    when(conversion.convert(anyObject(), eq(String.class))).thenReturn("12345-67890");

                    when(loader.getResource(eq("12345-67890"))).thenReturn(writeableResource);
                    output = mock(OutputStream.class);
                    when(writeableResource.getOutputStream()).thenReturn(output);

                    when(writeableResource.contentLength()).thenReturn(20L);
                });

                JustBeforeEach(() -> {
                    filesystemContentRepoImpl.setContent(entity, content);
                });


                Context("when the content already exists", () -> {
                    BeforeEach(() -> {
                        entity.setContentId("12345-67890");
                        when(writeableResource.exists()).thenReturn(true);
                    });

                    It("should use the conversion service to get a resource path", () -> {
                        verify(conversion).convert(anyObject(), anyObject());
                        verify(loader).getResource(eq("12345-67890"));
                    });

                    It("should change the content length", () -> {
                        assertThat(entity.getContentLen(), is(20L));
                    });

                    It("should write to the resource's outputstream", () -> {
                        verify(writeableResource).getOutputStream();
                        verify(output, times(1)).write(Matchers.<byte[]>any(), eq(0), eq(20));
                    });
                });

                Context("when the content does not already exist", () -> {
                    BeforeEach(() -> {
                        assertThat(entity.getContentId(), is(nullValue()));

                        File resourceFile = mock(File.class);
                        parent = mock(File.class);

                        when(writeableResource.getFile()).thenReturn(resourceFile);
                        when(resourceFile.getParentFile()).thenReturn(parent);
                    });

                    It("creates a directory for the parent", () -> {
                        verify(fileService).mkdirs(eq(parent));
                    });

                    It("should make a new UUID", () -> {
                        assertThat(entity.getContentId(), is(not(nullValue())));
                    });
                    It("should create a new resource", () -> {
                    	verify(loader).getResource(eq("12345-67890"));
                    });
                    It("should write to the resource's outputstream", () -> {
                        verify(writeableResource).getOutputStream();
                        verify(output, times(1)).write(Matchers.<byte[]>any(), eq(0), eq(20));
                    });
                });
            });

            Context("#getContent", () -> {
                BeforeEach(() -> {
                    entity = new TestEntity();
                    content = mock(InputStream.class);
                    entity.setContentId("abcd-efgh");
                  
                    when(conversion.convert(eq("abcd-efgh"), eq(String.class))).thenReturn("abcd-efgh");

                    when(loader.getResource(eq("abcd-efgh"))).thenReturn(writeableResource);
                    when(writeableResource.getInputStream()).thenReturn(content);
                });

                JustBeforeEach(() -> {
                	result = filesystemContentRepoImpl.getContent(entity);
                });
                
                Context("when the resource exists", () -> {
                    BeforeEach(() -> {
                        when(writeableResource.exists()).thenReturn(true);
                    });

                    It("should get content", () -> {
                        assertThat(result, is(content));
                    });
                });
                Context("when the resource does not exists", () -> {
                    BeforeEach(() -> {
                		nonExistentResource = mock(DeletableResource.class);
                		when(writeableResource.exists()).thenReturn(true);

                		when(loader.getResource(eq("/abcd/efgh"))).thenReturn(nonExistentResource);
                        when(loader.getResource(eq("abcd-efgh"))).thenReturn(nonExistentResource);
                    });

                    It("should not find the content", () -> {
                        assertThat(result, is(nullValue()));
                    });
                });
                Context("when the resource exists in the old location", () -> {
                	BeforeEach(() -> {
                		nonExistentResource = mock(DeletableResource.class);
                        when(loader.getResource(eq("/abcd/efgh"))).thenReturn(nonExistentResource);
                        when(nonExistentResource.exists()).thenReturn(false);

                        when(loader.getResource(eq("abcd-efgh"))).thenReturn(writeableResource);
                        when(writeableResource.exists()).thenReturn(true);
                	});
                	It("should check the new location and then the old", () -> {
                		InOrder inOrder = Mockito.inOrder(loader);
                		
                		inOrder.verify(loader).getResource(eq("abcd-efgh"));
                		inOrder.verifyNoMoreInteractions();
                	});
                    It("should get content", () -> {
                        assertThat(result, is(content));
                    });
                });
            });

            Context("#unsetContent", () -> {
                BeforeEach(() -> {
                    entity = new TestEntity();
                    entity.setContentId("abcd-efgh");
                    entity.setContentLen(100L);
                    deletableResource = mock(DeletableResource.class);
                });

                JustBeforeEach(() -> {
                	filesystemContentRepoImpl.unsetContent(entity);
                });

                Context("when the content exists in the new location", () -> {
                	BeforeEach(() -> {
                		when(conversion.convert(eq("abcd-efgh"), eq(String.class))).thenReturn("abcd-efgh");
                		
	            		when(loader.getResource(eq("abcd-efgh"))).thenReturn(deletableResource);
	            		when(deletableResource.exists()).thenReturn(true);
                	});
                	It("should unset content", () -> {
                	    verify(deletableResource).delete();
                		assertThat(entity.getContentId(), is(nullValue()));
                		assertThat(entity.getContentLen(), is(0L));
                	});
                });
                
                Context("when the content doesnt exist", () -> {
                	BeforeEach(() -> {
                        when(conversion.convert(eq("abcd-efgh"), eq(String.class))).thenReturn("abcd-efgh");

                		nonExistentResource = mock(DeletableResource.class);
                        when(loader.getResource(eq("abcd-efgh"))).thenReturn(nonExistentResource);
                        when(nonExistentResource.exists()).thenReturn(false);
                	});
                	It("should unset the content", () -> {
                		verify(nonExistentResource, never()).delete();
                		assertThat(entity.getContentId(), is(nullValue()));
                		assertThat(entity.getContentLen(), is(0L));
                	});
                });
            });
            
            Context("#getResource", () -> {
            	BeforeEach(() -> {
                    when(conversion.convert(eq("abcd-efgh"), eq(String.class))).thenReturn("/abcd/efgh");

            		resource = mock(Resource.class);
                    when(loader.getResource(eq("/abcd/efgh"))).thenReturn(resource);
            	});
            	JustBeforeEach(() -> {
            		filesystemContentRepoImpl.getResource("abcd-efgh");
            	});
            	It("should load resource from the placement provided location", () -> {
            		verify(loader).getResource(eq("/abcd/efgh"));
            	});
            });
        });
    }

    public static class TestEntity {
        @ContentId
        private String contentId;

        @ContentLength
        private long contentLen;

        public TestEntity() {
            this.contentId = null;
        }

        public TestEntity(String contentId) {
            this.contentId = new String(contentId);
        }

        public String getContentId() {
            return this.contentId;
        }

        public void setContentId(String contentId) {
            this.contentId = contentId;
        }

        public long getContentLen() {
            return contentLen;
        }

        public void setContentLen(long contentLen) {
            this.contentLen = contentLen;
        }
    }
}
