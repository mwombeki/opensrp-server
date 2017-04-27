package org.opensrp.register.mcare.report.mis1.maternityCare;


import org.opensrp.register.mcare.domain.Members;
import org.opensrp.register.mcare.report.mis1.ReportCalculator;

import java.util.Map;

public class ANCReportCalculator extends ReportCalculator {
    long visitOneCount = 0;


    public ANCReportCalculator(long startDateTime, long endDateTime) {
        super(startDateTime, endDateTime);
    }

    @Override
    public void calculate(Members member) {
        visitOneCount += addToVisitOneCount(member);
    }

    public long getVisitOneCount() {
        return visitOneCount;
    }

    private int addToVisitOneCount(Members member) {
        Map<String, String> anc1Visit = member.ANCVisit1();
        if(withInStartAndEndTime(anc1Visit)) {
            return 1;
        }
        return 0;
    }

    private boolean withInStartAndEndTime(Map<String, String> visitData) {
        if(visitData.containsKey(Members.CLIENT_VERSION_KEY)) {
            long clientVersion = Long.parseLong(visitData.get(Members.CLIENT_VERSION_KEY));
            if(clientVersion >= startDateTime && clientVersion <= endDateTime) {
                return true;
            }
        }
        return false;
    }


}
