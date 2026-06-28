package net.sylphian.minecraft.skills.db.repositories;

import net.sylphian.minecraft.skills.db.api.ISkillRepository;
import net.sylphian.minecraft.skills.db.dao.SkillDao;
import org.jdbi.v3.core.Jdbi;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * JDBI-backed implementation of {@link ISkillRepository}.
 * All operations are non-blocking, dispatched to the shared DB executor.
 */
public class SkillRepository implements ISkillRepository {

    private final Jdbi jdbi;
    private final ExecutorService executor;

    /**
     * @param jdbi     the JDBI instance
     * @param executor the shared async DB executor
     */
    public SkillRepository(Jdbi jdbi, ExecutorService executor) {
        this.jdbi = jdbi;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<Map<String, Long>> loadAll(UUID uuid) {
        return CompletableFuture.supplyAsync(() ->
                jdbi.withExtension(SkillDao.class, dao ->
                        dao.findAll(uuid.toString()).stream()
                                .collect(Collectors.toMap(SkillDao.SkillRow::skillId, SkillDao.SkillRow::xp))
                ), executor);
    }

    @Override
    public CompletableFuture<Long> loadOne(UUID uuid, String skillId) {
        return CompletableFuture.supplyAsync(() -> {
            Long result = jdbi.withExtension(SkillDao.class, dao ->
                    dao.findOne(uuid.toString(), skillId));
            return result != null ? result : 0L;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> upsertXP(UUID uuid, String skillId, long xp) {
        return CompletableFuture.runAsync(() ->
                jdbi.useExtension(SkillDao.class, dao ->
                        dao.upsertXP(uuid.toString(), skillId, xp)
                ), executor);
    }
}
