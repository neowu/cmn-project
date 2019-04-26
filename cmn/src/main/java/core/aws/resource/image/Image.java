package core.aws.resource.image;

import core.aws.resource.Resource;
import core.aws.resource.Resources;
import core.aws.resource.ec2.Instance;
import core.aws.task.ec2.BakeAMITask;
import core.aws.task.ec2.DeleteImageTask;
import core.aws.task.ec2.DeleteInstanceTask;
import core.aws.task.ec2.DeleteKeyPairTask;
import core.aws.task.ec2.DeleteSGTask;
import core.aws.task.ec2.DescribeImageTask;
import core.aws.util.Asserts;
import core.aws.util.Lists;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Tasks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.stream.Collectors;


/**
 * @author neo
 */
public class Image extends Resource implements AMI {
    public final NavigableMap<Integer, com.amazonaws.services.ec2.model.Image> remoteImages = new TreeMap<>();
    public final List<Instance> unfinishedBakeInstances = Lists.newArrayList();

    public AMI baseAMI;
    public Path playbook;
    public Optional<Path> packageDir = Optional.empty();
    public String namePrefix;

    public Image(String id) {
        super(id);
    }

    public int nextVersion() {
        return version().orElse(0) + 1;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String imageId() {
        if (remoteImages.isEmpty()) return baseAMI.imageId();
        return remoteImages.lastEntry().getValue().getImageId();
    }

    @Override
    public OptionalInt version() {
        if (remoteImages.isEmpty()) return OptionalInt.of(0);
        return OptionalInt.of(remoteImages.lastKey());
    }

    public String name() {
        return namePrefix + "-v" + nextVersion();
    }

    @Override
    public void validate(Resources resources) {
        int length = name().length();
        Asserts.isTrue(length >= 3 && length <= 128, "AMI names must be between 3 and 128 characters long, and may contain letters, numbers, '(', ')', '.', '-', '/' and '_'");

        Asserts.isTrue(Files.exists(playbook), "playbook does not exist, path={}", playbook);

        packageDir.ifPresent(path -> Asserts.isTrue(Files.exists(path), "packageDir does not exist, path={}", packageDir));
    }

    @Override
    protected void describeTasks(Tasks tasks) {
        tasks.add(new DescribeImageTask(this));
    }

    public void bakeTasks(Tasks tasks, boolean resume) {
        Instance resumeBakeInstance = null;

        if (resume && !unfinishedBakeInstances.isEmpty()) {
            resumeBakeInstance = unfinishedBakeInstances.remove(0);
        }

        for (Instance instance : unfinishedBakeInstances) {
            tasks.add(new DeleteInstanceTask(instance, instance.remoteInstances));

            if (instance.securityGroup != null)
                tasks.add(new DeleteSGTask(instance.securityGroup));

            if (instance.keyPair != null)
                tasks.add(new DeleteKeyPairTask(instance.keyPair));
        }

        tasks.add(new BakeAMITask(this, resumeBakeInstance));

        deleteAllFailedImages(tasks);

        while (remoteImages.size() >= 5) {
            Map.Entry<Integer, com.amazonaws.services.ec2.model.Image> entry = remoteImages.pollFirstEntry();
            tasks.add(new DeleteImageTask(this, entry.getValue()));
        }
    }

    private void deleteAllFailedImages(Tasks tasks) {
        List<Integer> failedImageVersions = remoteImages.entrySet().stream()
            .filter(entry -> !"available".equals(entry.getValue().getState()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        for (Integer failedImageVersion : failedImageVersions) {
            com.amazonaws.services.ec2.model.Image image = remoteImages.remove(failedImageVersion);
            tasks.add(new DeleteImageTask(this, image));
        }
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(id)
            .add("imageId", imageId())
            .add("version", version().orElse(0))
            .toString();
    }
}
