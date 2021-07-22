package moe.chikalar.recorder.repo;

import moe.chikalar.recorder.entity.RecordHistory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecordHistoryRepository extends CrudRepository<RecordHistory, Long>, PagingAndSortingRepository<RecordHistory, Long> {
}
