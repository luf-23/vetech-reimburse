package org.dep.reimburse.service;

import org.dep.reimburse.entity.ReimburseDoc;
import org.dep.reimburse.mapper.ReimburseDocMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 报销主表旁路缓存：读路径在此编排（先 Redis，未命中再 DB 并回填）；写路径由业务层先写 DB 后 {@link #evict}。
 */
@Service
public class ReimburseDocCacheService {

    static final String KEY_PREFIX = "reimburse:doc:";

    @Autowired
    private RedisTemplate<String, ReimburseDoc> reimburseDocRedisTemplate;
    @Autowired
    private ReimburseDocMapper docMapper;

    @Value("${reimburse.cache.doc-ttl:1h}")
    private Duration docTtl;

    /**
     * 旁路缓存读：先查 Redis，未命中再查 MySQL 并回填。
     */
    public ReimburseDoc loadById(Long id) {
        Optional<ReimburseDoc> cached = get(id);
        if (cached.isPresent()) {
            return cached.get();
        }
        ReimburseDoc doc = docMapper.selectById(id);
        if (doc != null) {
            put(doc);
        }
        return doc;
    }

    public Optional<ReimburseDoc> get(Long id) {
        ReimburseDoc cached = reimburseDocRedisTemplate.opsForValue().get(KEY_PREFIX + id);
        return Optional.ofNullable(cached);
    }

    public void put(ReimburseDoc doc) {
        if (doc == null || doc.getId() == null) {
            return;
        }
        reimburseDocRedisTemplate.opsForValue().set(KEY_PREFIX + doc.getId(), doc, docTtl);
    }

    public void evict(Long id) {
        reimburseDocRedisTemplate.delete(KEY_PREFIX + id);
    }
}
