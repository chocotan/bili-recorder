package moe.chikalar.bili.repo;

import moe.chikalar.bili.entity.RecordRoom;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecordRoomRepository extends CrudRepository<RecordRoom, Long> {
    public List<RecordRoom> findByStatus(String status);
}
