package com.qd.mail.service;

import com.filenet.api.collection.ContentElementList;
import com.filenet.api.constants.*;
import com.filenet.api.core.*;
import com.filenet.api.property.Properties;
import com.filenet.api.property.PropertyFilter;
import com.filenet.api.util.UserContext;
import com.qd.mail.json.MailConfig;
import com.qd.mail.json.util.FilenetConfig;
import com.qd.mail.service.util.MailProperties;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.security.auth.Subject;
import java.io.IOException;
import java.net.URLConnection;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;

@Slf4j
@RequiredArgsConstructor
@Component
public class FileNetP8Service {

    @Value("${filenet.username}")
    private String username;
    @Value("${filenet.password}")
    private String password;
    @Value("${filenet.baseUrl}")
    private String baseUrl;

    private ObjectStore objectStore;
    private Connection connection;
    private final PropertyFilter emptyPropertyFilter = new PropertyFilter();


    public void createFilenetDocument(MailConfig mailConfig, BodyPart bodyPart, HashMap<String, Object> mailProperties) {
        log.info("Handling attachment");
        Subject subject = getSubject();

        DocumentCreator documentCreator = new DocumentCreator(mailConfig, bodyPart, mailProperties);
        UserContext.doAs(subject, documentCreator);
    }

    private Subject getSubject() {
        log.debug("Authenticating in P8");
        connection = Factory.Connection.getConnection(baseUrl);
        return UserContext.createSubject(connection, username, password, null);
    }

    private void connectToOS(String osName) {
        EntireNetwork entireNetwork = Factory.EntireNetwork.fetchInstance(connection, emptyPropertyFilter);
        Domain domain = entireNetwork.get_LocalDomain();
//        Domain domain = Factory.Domain.fetchInstance(connection, "", emptyPropertyFilter);
        objectStore = Factory.ObjectStore.fetchInstance(domain, osName, emptyPropertyFilter);
    }

    @RequiredArgsConstructor
    private class DocumentCreator implements PrivilegedExceptionAction<Object> {
        private final MailConfig mailConfig;
        private final BodyPart bodyPart;
        private final HashMap<String, Object> mailProperties;

        @Override
        public Object run() throws Exception {
            FilenetConfig filenetConfig = mailConfig.filenetConfig();

            connectToOS(mailConfig.filenetConfig().osName());

            String fileName = bodyPart.getFileName();
            Document document = addDocumentToFileNet(fileName, bodyPart, filenetConfig.documentClassName(), filenetConfig.propertiesMapping(), mailProperties);
            fileInDocument(filenetConfig.targetFolder(), fileName, document);

            return null;
        }
    }

    private void fileInDocument(String targetFolderPath, String fileName, Document document) {
        Folder targetFolder = Factory.Folder.fetchInstance(objectStore, targetFolderPath, emptyPropertyFilter);
        ReferentialContainmentRelationship referentialContainmentRelationship = targetFolder.file(document, AutoUniqueName.AUTO_UNIQUE, fileName, DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
        referentialContainmentRelationship.save(RefreshMode.NO_REFRESH);
        log.info("Document {} filed into Folder {}", document.get_Id(), targetFolderPath);
    }

    private Document addDocumentToFileNet(String fileName, BodyPart bodyPart, String documentClassName, HashMap<String, String> propertiesMapping, HashMap<String, Object> mailProperties) throws MessagingException, IOException {
        Document document = Factory.Document.createInstance(objectStore, documentClassName);

        ContentElementList contentElementList = Factory.ContentElement.createList();
        ContentTransfer contentTransfer = Factory.ContentTransfer.createInstance();
        contentTransfer.setCaptureSource(bodyPart.getInputStream());
        contentTransfer.set_RetrievalName(fileName);
        contentElementList.add(contentTransfer);
        document.set_ContentElements(contentElementList);

        String contentTypeFromName = URLConnection.guessContentTypeFromName(fileName);
        document.set_MimeType(contentTypeFromName);

        mapProperties(fileName, document, propertiesMapping, mailProperties);

        document.checkin(AutoClassify.AUTO_CLASSIFY, CheckinType.MAJOR_VERSION);
        document.save(RefreshMode.NO_REFRESH);
        document.fetchProperties(emptyPropertyFilter);

        log.info("New Document added to P8,Id: {}, FileName: {}, mimetype: {}", document.get_Id(), fileName, contentTypeFromName);

        return document;
    }

    private static void mapProperties(String fileName, Document document, HashMap<String, String> propertiesMapping, HashMap<String, Object> mailProperties) {
        Properties properties = document.getProperties();
        properties.putValue("DocumentTitle", fileName);

        if (propertiesMapping.containsKey(MailProperties.SUBJECT)) {
            properties.putValue(propertiesMapping.get(MailProperties.SUBJECT), mailProperties.get(MailProperties.SUBJECT).toString());
        }
        if (propertiesMapping.containsKey(MailProperties.SENT_DATE)) {
            properties.putValue(propertiesMapping.get(MailProperties.SENT_DATE), mailProperties.get(MailProperties.SENT_DATE).toString());
        }
        if (propertiesMapping.containsKey(MailProperties.RECEIVED_DATE)) {
            properties.putValue(propertiesMapping.get(MailProperties.RECEIVED_DATE), mailProperties.get(MailProperties.RECEIVED_DATE).toString());
        }
        if (propertiesMapping.containsKey(MailProperties.TO)) {
            properties.putValue(propertiesMapping.get(MailProperties.TO), StringUtils.join(mailProperties.get(MailProperties.TO), "; "));
        }
        if (propertiesMapping.containsKey(MailProperties.FROM)) {
            properties.putValue(propertiesMapping.get(MailProperties.FROM), StringUtils.join(mailProperties.get(MailProperties.FROM), "; "));
        }
    }
}
