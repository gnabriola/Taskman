package com.taskman.label;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class LabelService implements LabelApi {

	private final LabelRepository labels;

	private final ApplicationEventPublisher events;

	LabelService(LabelRepository labels, ApplicationEventPublisher events) {
		this.labels = labels;
		this.events = events;
	}

	@Override
	public void requireExisting(Set<UUID> labelIds) {
		if (labelIds.isEmpty()) {
			return;
		}
		Set<UUID> missing = new HashSet<>(labelIds);
		labels.findExistingIds(labelIds).forEach(missing::remove);
		if (!missing.isEmpty()) {
			throw new LabelNotFoundException(missing.iterator().next());
		}
	}

	@Override
	public Optional<UUID> findIdByName(String name) {
		return labels.findByNameIgnoreCase(name).map(Label::getId);
	}

	@Override
	@Transactional
	public List<UUID> findOrCreateByNames(Collection<String> names) {
		List<UUID> ids = new ArrayList<>();
		for (String name : names) {
			Label label = labels.findByNameIgnoreCase(name).orElseGet(() -> labels.save(new Label(name)));
			ids.add(label.getId());
		}
		return ids;
	}

	List<LabelResponse> findAll() {
		return labels.findAllByOrderByNameAsc().stream().map(LabelResponse::from).toList();
	}

	@Transactional
	LabelResponse create(LabelRequest request) {
		String name = request.name().strip();
		if (labels.findByNameIgnoreCase(name).isPresent()) {
			throw new LabelNameTakenException(name);
		}
		return LabelResponse.from(labels.save(new Label(name)));
	}

	@Transactional
	LabelResponse rename(UUID id, LabelRequest request) {
		Label label = load(id);
		String name = request.name().strip();
		labels.findByNameIgnoreCase(name)
			.filter(other -> !other.getId().equals(id))
			.ifPresent(other -> {
				throw new LabelNameTakenException(name);
			});
		label.rename(name);
		return LabelResponse.from(label);
	}

	@Transactional
	void delete(UUID id) {
		Label label = load(id);
		// Listeners run synchronously in this transaction, before the row is removed.
		events.publishEvent(new LabelDeletedEvent(id));
		labels.delete(label);
	}

	private Label load(UUID id) {
		return labels.findById(id).orElseThrow(() -> new LabelNotFoundException(id));
	}

}
