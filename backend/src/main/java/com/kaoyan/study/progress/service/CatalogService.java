package com.kaoyan.study.progress.service;

import com.kaoyan.study.common.exception.*;
import com.kaoyan.study.learning.dto.StudyRecordView;
import com.kaoyan.study.learning.service.StudyRecordService;
import com.kaoyan.study.plan.service.TaskService;
import com.kaoyan.study.progress.dto.CatalogContracts.*;
import com.kaoyan.study.progress.mapper.CatalogMapper;
import com.kaoyan.study.progress.mapper.CatalogMapper.*;
import com.kaoyan.study.settings.service.SubjectService;
import com.kaoyan.study.settings.service.StudyUnitService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CatalogService {
    private final CatalogMapper mapper;
    private final StudyRecordService records;
    private final TaskService tasks;
    private final SubjectService subjects;
    private final StudyUnitService units;
    private final JsonMapper json = JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT).enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private static final Set<String> DIRECTIONS = Set.of("MATH","ENGLISH","POLITICS","CS408");
    public CatalogService(CatalogMapper mapper, StudyRecordService records, TaskService tasks, SubjectService subjects, StudyUnitService units) {
        this.mapper=mapper; this.records=records; this.tasks=tasks; this.subjects=subjects; this.units=units;
    }

    private record State(List<CourseRow> courses, List<NodeRow> nodes, List<TaskLink> tasks, List<RecordLink> records) {}
    private State state() { return new State(mapper.courses(),mapper.nodes(),mapper.taskLinks(),mapper.recordLinks()); }
    private static <T> Map<Long,T> byId(List<T> values, java.util.function.Function<T,Long> key) {
        return values.stream().collect(Collectors.toMap(key, x->x));
    }
    /** 显式归属优先；关联任务/单位变化后失效，回到待归类。不会按标题猜测。 */
    private Map<Long,NodeRow> resolve(List<StudyRecordView> rows, State state) {
        var nodes=byId(state.nodes(),NodeRow::id); var courses=byId(state.courses(),CourseRow::id);
        var taskLinks=state.tasks().stream().collect(Collectors.toMap(TaskLink::taskId,TaskLink::nodeId));
        var recordLinks=byId(state.records(),RecordLink::recordId);
        var result=new HashMap<Long,NodeRow>();
        for(var r:rows) {
            var explicit=recordLinks.get(r.id());
            Long nodeId=explicit==null ? taskLinks.get(r.taskId())
                    : Objects.equals(explicit.sourceTaskId(),r.taskId()) ? explicit.nodeId() : null;
            var node=nodes.get(nodeId); var course=node==null ? null:courses.get(node.courseId());
            if(node!=null && node.totalAmount()!=null && course!=null
                    && Objects.equals(r.subjectId(),course.subjectId()) && Objects.equals(r.unitId(),course.unitId())) result.put(r.id(),node);
        }
        return result;
    }
    @Transactional(readOnly=true)
    public Map<Long,RecordClassification> classifications(List<StudyRecordView> rows) {
        var s=state(); var courses=byId(s.courses(),CourseRow::id);
        var result=new HashMap<Long,RecordClassification>();
        resolve(rows,s).forEach((id,node)->result.put(id,new RecordClassification(node.courseId(),courses.get(node.courseId()).title())));
        return result;
    }
    @Transactional(readOnly=true)
    public Library library() {
        var s=state(); var rows=records.list(null,null,null,null); var assigned=resolve(rows,s);
        var totals=new HashMap<Long,BigDecimal>();
        for(var r:rows) if(assigned.containsKey(r.id()) && r.amount()!=null) totals.merge(assigned.get(r.id()).id(),r.amount(),BigDecimal::add);
        var views=new ArrayList<CourseView>();
        for(var c:s.courses()) {
            var nodes=s.nodes().stream().filter(n->n.courseId().equals(c.id())).toList();
            var leaves=nodes.stream().filter(n->n.totalAmount()!=null).toList();
            var nv=nodes.stream().map(n->new NodeView(n.id(),n.nodeKey(),n.parentKey(),n.title(),n.totalAmount(),
                    nodeCompleted(n,nodes,totals),s.tasks().stream().filter(t->t.nodeId().equals(n.id())).map(TaskLink::taskId).toList(),n.sortOrder())).toList();
            views.add(new CourseView(c.id(),c.courseKey(),c.direction(),c.branch(),c.title(),c.subjectId(),c.unitId(),units.get(c.unitId()).getName(),
                    leaves.stream().map(NodeRow::totalAmount).reduce(BigDecimal.ZERO,BigDecimal::add),
                    leaves.stream().map(n->totals.getOrDefault(n.id(),BigDecimal.ZERO).min(n.totalAmount())).reduce(BigDecimal.ZERO,BigDecimal::add),
                    leaves.stream().map(n->totals.getOrDefault(n.id(),BigDecimal.ZERO)).reduce(BigDecimal.ZERO,BigDecimal::add),nv));
        }
        var cumulative=new LinkedHashMap<String,Quantity>();
        for(var r:rows) if(r.subjectId()!=null && r.unitId()!=null && r.amount()!=null) {
            String key=r.subjectId()+":"+r.unitId(); var old=cumulative.get(key);
            cumulative.put(key,new Quantity(directionOf(r.subjectId(),s.courses()),r.subjectId(),r.subjectName(),r.unitId(),r.unitName(),
                    r.amount().add(old==null ? BigDecimal.ZERO:old.amount())));
        }
        return new Library(mapper.revision(),views,new ArrayList<>(cumulative.values()),rows.stream().filter(r->r.amount()!=null && !assigned.containsKey(r.id())).toList());
    }
    private BigDecimal nodeCompleted(NodeRow node,List<NodeRow> nodes,Map<Long,BigDecimal> totals) {
        if(node.totalAmount()!=null) return totals.getOrDefault(node.id(),BigDecimal.ZERO);
        return nodes.stream().filter(n->Objects.equals(n.parentKey(),node.nodeKey())).map(n->nodeCompleted(n,nodes,totals)).reduce(BigDecimal.ZERO,BigDecimal::add);
    }
    private String directionOf(Long subjectId,List<CourseRow> courses) {
        var course=courses.stream().filter(c->c.subjectId().equals(subjectId)).findFirst();
        if(course.isPresent()) return course.get().direction();
        String name=subjects.get(subjectId).getName();
        if(name.contains("数学") || name.contains("高数") || name.contains("线代") || name.contains("概率")) return "MATH";
        if(name.contains("英语")) return "ENGLISH";
        if(name.contains("政治")) return "POLITICS";
        if(name.contains("408") || name.contains("数据结构") || name.contains("组成") || name.contains("操作系统") || name.contains("计网") || name.contains("计算机网络")) return "CS408";
        return "UNASSIGNED";
    }
    @Transactional(readOnly=true)
    public List<TaskCandidate> candidates() {
        var links=mapper.taskLinks().stream().collect(Collectors.toMap(TaskLink::taskId,TaskLink::nodeId));
        return tasks.list(null,null,null).stream().map(t->new TaskCandidate(t.getId(),t.getTitle(),t.getSubjectId(),t.getUnitId(),t.getPlannedAmount(),t.getVersion(),links.get(t.getId()))).toList();
    }
    public Document parse(String source) {
        if(source==null || source.getBytes(StandardCharsets.UTF_8).length>2_000_000) invalid("目录文件不能为空或超过 2 MB");
        try {
            var tree=json.readTree(source);
            checkTypes(tree,"");
            return json.treeToValue(tree,Document.class);
        } catch(BusinessException e) {throw e;} catch(Exception e) {throw new BusinessException("INVALID_CATALOG","目录 JSON 格式不正确，请对照导入说明检查",e);}
    }
    private void checkTypes(tools.jackson.databind.JsonNode node,String key) {
        if(node==null || node.isNull()) return;
        if(Set.of("key","parentKey","title","direction","branch").contains(key) && !node.isString()) invalid(key+" 必须是文本");
        if(Set.of("subjectId","unitId","sortOrder","schemaVersion").contains(key) && !node.isIntegralNumber()) invalid(key+" 必须是整数");
        if(key.equals("totalAmount") && !node.isNumber()) invalid("totalAmount 必须是数值");
        if(node.isObject()) for(var name:node.propertyNames()) checkTypes(node.get(name),name);
        if(node.isArray()) for(var child:node) { if(child.isNull()) invalid("列表不能含 null"); if(key.equals("taskIds") && !child.isIntegralNumber()) invalid("taskIds 必须是整数列表"); checkTypes(child,""); }
    }
    private List<CourseChange> validate(Document doc,State s) {
        if(doc==null || !Integer.valueOf(1).equals(doc.schemaVersion()) || doc.courses()==null || doc.courses().isEmpty() || doc.courses().size()>100) invalid("版本必须为 1，courses 须包含 1—100 门课程");
        var changes=new ArrayList<CourseChange>(); var courseKeys=new HashSet<String>(); var taskIds=new HashSet<Long>();
        var existingCourses=s.courses().stream().collect(Collectors.toMap(CourseRow::courseKey,c->c));
        var incomingTargets=new HashMap<Long,String>();
        int nodeCount=0;
        for(var c:doc.courses()) {
            if(c==null) invalid("课程不能为空");
            key(c.key()); text(c.title(),200); text(c.branch(),100);
            if(!DIRECTIONS.contains(Objects.toString(c.direction(),""))) invalid("direction 须为 MATH、ENGLISH、POLITICS 或 CS408");
            if(!courseKeys.add(c.key())) invalid("课程 key 重复");
            if(c.subjectId()==null || c.unitId()==null) invalid("课程必须指定现有科目和单位");
            subjects.get(c.subjectId()); var unit=units.get(c.unitId()); var old=existingCourses.get(c.key());
            if(old!=null && (!old.subjectId().equals(c.subjectId()) || !old.unitId().equals(c.unitId()))) invalid("已有课程不能改变科目或单位，请新建课程 key");
            String previous=incomingTargets.putIfAbsent(c.subjectId(),c.direction());
            if(previous!=null && !previous.equals(c.direction())) invalid("同一科目不能归入两个大方向");
            for(var other:s.courses()) if(other.subjectId().equals(c.subjectId()) && !other.courseKey().equals(c.key()) && !other.direction().equals(c.direction())) invalid("同一科目不能归入两个大方向");
            if(c.nodes()==null || c.nodes().isEmpty()) invalid("每门课程必须提供目录");
            nodeCount+=c.nodes().size(); if(nodeCount>2000) invalid("单次最多导入 2000 个目录项");
            var merged=new LinkedHashMap<String,NodeInput>();
            if(old!=null) for(var n:s.nodes()) if(n.courseId().equals(old.id())) merged.put(n.nodeKey(),new NodeInput(n.nodeKey(),n.parentKey(),n.title(),n.totalAmount(),n.sortOrder(),s.tasks().stream().filter(t->t.nodeId().equals(n.id())).map(TaskLink::taskId).toList()));
            BigDecimal before=merged.values().stream().map(NodeInput::totalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
            int added=0; var keys=new HashSet<String>();
            for(var n:c.nodes()) {
                if(n==null) invalid("目录项不能为空");
                key(n.key()); text(n.title(),200); if(n.parentKey()!=null) key(n.parentKey());
                if(!keys.add(n.key())) invalid("目录 key 重复");
                if(n.totalAmount()!=null && (n.totalAmount().signum()<=0 || n.totalAmount().scale()>6 || n.totalAmount().precision()-n.totalAmount().scale()>12)) invalid("目录总量须为正数，最多 6 位小数及 12 位整数");
                if(n.sortOrder()!=null && (n.sortOrder()<0 || n.sortOrder()>100000)) invalid("排序值须在 0—100000 内");
                if(n.taskIds()==null || n.taskIds().size()>2000 || n.taskIds().stream().anyMatch(Objects::isNull)) invalid("taskIds 必须是任务 ID 列表；无关联填 []");
                if(!merged.containsKey(n.key())) added++;
                merged.put(n.key(),n);
                for(var taskId:n.taskIds()) {
                    if(!taskIds.add(taskId)) invalid("一个任务只能关联一个目录项");
                    var task=tasks.get(taskId);
                    if(!task.getSubjectId().equals(c.subjectId()) || !task.getUnitId().equals(c.unitId())) invalid("关联任务的科目和单位必须与课程一致");
                    var link=s.tasks().stream().filter(t->t.taskId().equals(taskId)).findFirst();
                    if(link.isPresent()) {
                        var target=s.nodes().stream().filter(x->x.id().equals(link.get().nodeId())).findFirst().orElseThrow();
                        if(old==null || !target.courseId().equals(old.id()) || !target.nodeKey().equals(n.key())) invalid("任务已有其他目录归属；请在目录关联入口调整，不能导入时悄悄转移");
                    }
                }
            }
            for(var n:merged.values()) {
                if(n.parentKey()!=null && !merged.containsKey(n.parentKey())) invalid("父目录不存在："+n.parentKey());
                boolean hasChildren=merged.values().stream().anyMatch(x->Objects.equals(x.parentKey(),n.key()));
                if(hasChildren && (n.totalAmount()!=null || !n.taskIds().isEmpty())) invalid("章节容器不能同时带数量或任务，请把数量及关联放在末级条目");
                if(!hasChildren && n.totalAmount()==null) invalid("末级目录必须填写完整总量");
                var seen=new HashSet<String>(); var cursor=n;
                while(cursor!=null) {if(!seen.add(cursor.key()) || seen.size()>8) invalid("目录有循环或超过 8 层"); cursor=cursor.parentKey()==null ? null:merged.get(cursor.parentKey());}
                if(old!=null && hasChildren) {
                    var existing=s.nodes().stream().filter(x->x.courseId().equals(old.id()) && x.nodeKey().equals(n.key())).findFirst();
                    if(existing.isPresent() && s.records().stream().anyMatch(r->r.nodeId().equals(existing.get().id()))) invalid("已有学习归属的末级目录不能改为章节容器");
                    if(existing.isPresent() && s.tasks().stream().anyMatch(t->t.nodeId().equals(existing.get().id()))) invalid("已有任务归属的末级目录不能改为章节容器，请先解除关联");
                }
            }
            BigDecimal after=merged.values().stream().map(NodeInput::totalAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);
            if(after.precision()-after.scale()>12) invalid("课程总量过大");
            changes.add(new CourseChange(c.title(),unit.getName(),before,after,added));
        }
        return changes;
    }
    private String context(Document doc) {
        var ids=doc.courses().stream().flatMap(c->c.nodes().stream()).flatMap(n->n.taskIds().stream()).distinct().sorted().toList();
        String value=json.writeValueAsString(ids.stream().map(tasks::get).toList())+json.writeValueAsString(subjects.list(false))+json.writeValueAsString(units.list(false));
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(Exception e) {throw new IllegalStateException(e);}
    }
    @Transactional
    public Preview preview(String source) {
        mapper.lock(); var doc=parse(source); var changes=validate(doc,state());
        String token=UUID.randomUUID().toString(); long revision=mapper.revision(); var expires=LocalDateTime.now().plusMinutes(15);
        mapper.clearExpired(); mapper.insertPreview(token,revision,json.writeValueAsString(doc),context(doc),expires);
        return new Preview(token,revision,expires,doc,changes);
    }
    @Transactional
    public Library confirm(String token) {
        mapper.lock(); var p=mapper.preview(token);
        if(p==null) throw new NotFoundException("目录预览不存在，请重新预览");
        if(p.applied()==1) return library(); // 网络重试不再次增加目录或总量
        if(p.expiresAt().isBefore(LocalDateTime.now()) || p.revision()!=mapper.revision()) stale();
        var doc=parse(p.payload());
        doc.courses().stream().flatMap(c->c.nodes().stream()).flatMap(n->n.taskIds().stream()).distinct().sorted().forEach(tasks::requireExistingForUpdate);
        if(!p.contextHash().equals(context(doc))) stale();
        validate(doc,state());
        for(var c:doc.courses()) {
            var existing=mapper.courses().stream().filter(x->x.courseKey().equals(c.key())).findFirst();
            if(existing.isEmpty()) mapper.insertCourse(c.key(),c.direction(),c.branch(),c.title(),c.subjectId(),c.unitId());
            else mapper.updateCourse(existing.get().id(),c.direction(),c.branch(),c.title());
            Long courseId=mapper.courses().stream().filter(x->x.courseKey().equals(c.key())).findFirst().orElseThrow().id();
            for(var n:c.nodes()) {
                mapper.upsertNode(courseId,n.key(),n.parentKey(),n.title(),n.totalAmount(),n.sortOrder()==null ? 0:n.sortOrder());
                Long nodeId=mapper.nodes().stream().filter(x->x.courseId().equals(courseId) && x.nodeKey().equals(n.key())).findFirst().orElseThrow().id();
                mapper.clearTasks(nodeId); for(var taskId:n.taskIds()) mapper.linkTask(taskId,nodeId);
            }
        }
        mapper.touch(); mapper.applied(token); return library();
    }
    @Transactional
    public Library assign(Long recordId,AssignRequest request) {
        checkRevision(request.revision());
        var r=records.lockForClassification(recordId,request.recordVersion());
        if(request.nodeId()==null) mapper.unassign(recordId);
        else {
            var s=state(); var node=s.nodes().stream().filter(n->n.id().equals(request.nodeId())).findFirst().orElseThrow(()->new NotFoundException("目录不存在"));
            var c=s.courses().stream().filter(x->x.id().equals(node.courseId())).findFirst().orElseThrow();
            if(node.totalAmount()==null || r.amount()==null || !Objects.equals(r.subjectId(),c.subjectId()) || !Objects.equals(r.unitId(),c.unitId())) invalid("只能归类到同科目、同单位的末级目录");
            mapper.assign(recordId,node.id(),r.taskId());
        }
        mapper.touch(); return library();
    }
    @Transactional
    public Library linkTasks(Long nodeId,TaskLinkRequest request) {
        checkRevision(request.revision()); var s=state();
        var node=s.nodes().stream().filter(n->n.id().equals(nodeId)).findFirst().orElseThrow(()->new NotFoundException("目录不存在"));
        var c=s.courses().stream().filter(x->x.id().equals(node.courseId())).findFirst().orElseThrow();
        if(node.totalAmount()==null || request.taskIds()==null || request.taskIds().size()>2000 || request.taskIds().stream().anyMatch(Objects::isNull) || new HashSet<>(request.taskIds()).size()!=request.taskIds().size()) invalid("只能关联末级目录，任务列表不能重复");
        for(var id:request.taskIds().stream().sorted().toList()) {
            var t=tasks.requireExistingForUpdate(id);
            if(!t.getSubjectId().equals(c.subjectId()) || !t.getUnitId().equals(c.unitId())) invalid("任务科目和单位不一致");
            if(s.tasks().stream().anyMatch(x->x.taskId().equals(id) && !x.nodeId().equals(nodeId))) invalid("任务已归属于其他目录，请先解除原关联");
        }
        mapper.clearTasks(nodeId); request.taskIds().forEach(id->mapper.linkTask(id,nodeId)); mapper.touch(); return library();
    }
    private void checkRevision(Long revision) {long current=mapper.lock(); if(revision==null || revision!=current) stale();}
    private void key(String key) {if(key==null || !key.matches("[A-Za-z0-9_-]{1,64}")) invalid("key 只能包含 1—64 位字母、数字、下划线或短横线");}
    private void text(String value,int max) {if(value==null || value.isBlank() || value.length()>max) invalid("名称不能为空且不能超过 "+max+" 字");}
    private void invalid(String message) {throw new BusinessException("INVALID_CATALOG",message);}
    private void stale() {throw new ConflictException("CATALOG_STALE","目录、任务或科目数据已变化，或预览已过期，请刷新并重新预览");}
}
