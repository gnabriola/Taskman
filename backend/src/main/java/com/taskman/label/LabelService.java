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
	public void requireExisting(UUID ownerId, Set<UUID> labelIds) {
		if (labelIds.isEmpty()) {
			return;
		}
		Set<UUID> missing = new HashSet<>(labelIds);
		labels.findExistingIds(ownerId, labelIds).forEach(missing::remove);
		if (!missing.isEmpty()) {
			throw new LabelNotFoundException(missing.iterator().next());
		}
	}

	@Override
	public Optional<UUID> findIdByName(UUID ownerId, String name) {
		return labels.findByOwnerIdAndNameIgnoreCase(ownerId, name).map(Label::getId);
	}

	@Override
	@Transactional
	public List<UUID> findOrCreateByNames(UUID ownerId, Collection<String> names) {
		List<UUID> ids = new ArrayList<>();
		for (String name : names) {
			Label label = labels.findByOwnerIdAndNameIgnoreCase(ownerId, name)
				.orElseGet(() -> labels.save(new Label(ownerId, name)));
			ids.add(label.getId());
		}
		return ids;
	}

	List<LabelResponse> findAll(UUID ownerId) {
		return labels.findAllByOwnerIdOrderByNameAsc(ownerId).stream().map(LabelResponse::from).toList();
	}

	@Transactional
	LabelResponse create(UUID ownerId, LabelRequest request) {
		String name = request.name().strip();
		if (labels.findByOwnerIdAndNameIgnoreCase(ownerId, name).isPresent()) {
			throw new LabelNameTakenException(name);
		}
		return LabelResponse.from(labels.save(new Label(ownerId, name)));
	}

	@Transactional
	LabelResponse rename(UUID ownerId, UUID id, LabelRequest request) {
		Label label = load(ownerId, id);
		String name = request.name().strip();
		labels.findByOwnerIdAndNameIgnoreCase(ownerId, name)
			.filter(other -> !other.getId().equals(id))
			.ifPresent(other -> {
				throw new LabelNameTakenException(name);
			});
		label.rename(name);
		return LabelResponse.from(label);
	}

	@Transactional
	void delete(UUID ownerId, UUID id) {
		Label label = load(ownerId, id);
		// Listeners run synchronously in this transaction, before the row is removed.
		events.publishEvent(new LabelDeletedEvent(id));
		labels.delete(label);
	}

	/** Another user's label is reported as not found, so ids cannot be probed. */
	private Label load(UUID ownerId, UUID id) {
		return labels.findByIdAndOwnerId(id, ownerId).orElseThrow(() -> new LabelNotFoundException(id));
	}

}
