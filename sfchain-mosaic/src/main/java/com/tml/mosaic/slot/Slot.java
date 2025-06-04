package com.tml.mosaic.slot;

import com.tml.mosaic.core.tools.guid.DotNotationId;
import com.tml.mosaic.core.tools.guid.GUID;
import com.tml.mosaic.core.tools.guid.UniqueEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * 放置Cube的槽，用于引入Cube插件来进行部署和使用
 */
public class Slot extends UniqueEntity {

    @Getter
    private SetupCubeInfo setupCubeInfo;

    public Slot(String slotName) {
        super(new DotNotationId(slotName));
    }

    public Slot(DotNotationId id) {
        super(id);
    }

    public boolean Setup(SetupCubeInfo setupCubeInfo) {
        if(SetupCubeInfo.reliabilityVerify(setupCubeInfo)){
            this.setupCubeInfo = setupCubeInfo;
            return true;
        }
        return false;
    }

    public boolean UnSetup() {
        this.setupCubeInfo = null;
        return true;
    }

    /**
     * 安装的Cube信息
     */
    @NoArgsConstructor
    public static class SetupCubeInfo {

        @Getter
        @Setter
        // 方块唯一Id
        private GUID cubeId;

        // 调用的方法名称
        @Getter
        @Setter
        private GUID methodId;

        /**
         * 可靠性校验，校验SetupCubeInfo是否可用
         */
        public static boolean reliabilityVerify(SetupCubeInfo setupCubeInfo){
            return Objects.nonNull(setupCubeInfo) && Stream.of(setupCubeInfo.getCubeId()).allMatch(Objects::nonNull);
        }
    }

}
