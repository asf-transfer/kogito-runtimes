package org.kie.kogito.persistence.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.kie.kogito.process.MutableProcessInstances;
import org.kie.kogito.process.Process;
import org.kie.kogito.process.ProcessInstance;
import org.kie.kogito.process.ProcessInstanceDuplicatedException;
import org.kie.kogito.process.impl.AbstractProcessInstance;
import org.kie.kogito.process.impl.marshalling.ProcessInstanceMarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"rawtypes"})
public class FileSystemProcessInstances implements MutableProcessInstances {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemProcessInstances.class);

	private static final String PI_DESCRIPTION = "ProcessInstanceDescription";
    private Process<?> process;
	private Path storage;

	private ProcessInstanceMarshaller marshaller;

	public FileSystemProcessInstances(Process<?> process, Path storage) {
		this(process, storage, new ProcessInstanceMarshaller());
	}

	public FileSystemProcessInstances(Process<?> process, Path storage, ProcessInstanceMarshaller marshaller) {
		this.process = process;
		this.storage = Paths.get(storage.toString(), process.id());
		this.marshaller = marshaller;

		try {
			Files.createDirectories(this.storage);
		} catch (IOException e) {
			throw new RuntimeException("Unable to create directories for file based storage of process instances", e);
		}
	}

	@Override
	public Optional findById(String id) {
		String resolvedId = resolveId(id);
		Path processInstanceStorage = Paths.get(storage.toString(), resolvedId);

        if (Files.notExists(processInstanceStorage)) {
            return Optional.empty();
        }
    	return (Optional<? extends ProcessInstance>) Optional.of(marshaller.unmarshallProcessInstance(readBytesFromFile(processInstanceStorage), process));

	}

	@Override
	public Collection values() {
		try {
			return Files.walk(storage)
					.filter(file -> !Files.isDirectory(file))
					.map(f -> marshaller.unmarshallProcessInstance(readBytesFromFile(f), process))
					.collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException("Unable to read process instances ", e);
		}
	}

	@Override
	public boolean exists(String id) {
		return Files.exists(Paths.get(storage.toString(), resolveId(id)));
	}

	@SuppressWarnings("unchecked")
	@Override
	public void create(String id, ProcessInstance instance) {
		if (isActive(instance)) {
			String resolvedId = resolveId(id);
			Path processInstanceStorage = Paths.get(storage.toString(), resolvedId);

			if (Files.exists(processInstanceStorage)) {
				throw new ProcessInstanceDuplicatedException(id);
			}
			storeProcessInstance(processInstanceStorage, instance);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void update(String id, ProcessInstance instance) {
		if (isActive(instance)) {
			String resolvedId = resolveId(id);
			Path processInstanceStorage = Paths.get(storage.toString(), resolvedId);

			if (Files.exists(processInstanceStorage)) {
				storeProcessInstance(processInstanceStorage, instance);
			}
		}
	}

	@Override
	public void remove(String id) {
		Path processInstanceStorage = Paths.get(storage.toString(), resolveId(id));

		try {
			Files.deleteIfExists(processInstanceStorage);
		} catch (IOException e) {
			throw new RuntimeException("Unable to remove process instance with id " + id, e);
		}

	}
	
	protected void storeProcessInstance(Path processInstanceStorage, ProcessInstance<?> instance) {
	    try {
            byte[] data = marshaller.marhsallProcessInstance(instance);
            Files.write(processInstanceStorage, data, StandardOpenOption.CREATE_NEW);
            setMetadata(processInstanceStorage, PI_DESCRIPTION, instance.description());
            
            disconnect(processInstanceStorage, instance);
        } catch (IOException e) {
            throw new RuntimeException("Unable to store process instance with id " + instance.id(), e);
        }
	}

	protected byte[] readBytesFromFile(Path processInstanceStorage) {
		try {
			return Files.readAllBytes(processInstanceStorage);
		} catch (IOException e) {
			throw new RuntimeException("Unable to read process instance from " + processInstanceStorage, e);
		}
	}

	protected void disconnect(Path processInstanceStorage, ProcessInstance instance) {
		((AbstractProcessInstance<?>) instance).internalRemoveProcessInstance(() -> {

			try {
				byte[] reloaded = readBytesFromFile(processInstanceStorage);

	            return ((AbstractProcessInstance<?>)marshaller.unmarshallProcessInstance(reloaded, process, (AbstractProcessInstance<?>) instance)).internalGetProcessInstance();
			} catch (RuntimeException e) {
			    LOGGER.error("Unexpected exception thrown when reloading process instance {}", instance.id(), e);
				return null;
			}


        });
	}
	
	protected String getMetadata(Path file, String key) {
        
        UserDefinedFileAttributeView view = Files.getFileAttributeView(file, UserDefinedFileAttributeView.class);
        if (view != null) {
            try {
                java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocate(view.size(key));
                view.read(key, bb);
                bb.flip();
                return Charset.defaultCharset().decode(bb).toString();
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }
	
	protected boolean setMetadata(Path file, String key, String value) {
        
        UserDefinedFileAttributeView view = Files.getFileAttributeView(file, UserDefinedFileAttributeView.class);
        if (view != null) {
            try {
                if (value != null) {
                    view.write(key, Charset.defaultCharset().encode(value));
                } else {
                    view.delete(key);
                }
                return true;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return false;
    }

}
