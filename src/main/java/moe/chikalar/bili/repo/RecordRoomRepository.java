package moe.chikalar.bili.repo;

import moe.chikalar.bili.entity.RecordRoom;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecordRoomRepository extends CrudRepository<RecordRoom, Long> {
}
