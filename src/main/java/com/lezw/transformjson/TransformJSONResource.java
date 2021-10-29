/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lezw.transformjson;

import com.bazaarvoice.jolt.JoltTransform;
import com.bazaarvoice.jolt.JsonUtils;
import com.lezw.transformjson.dto.JoltSpecificationDTO;
import com.lezw.transformjson.dto.ValidationDTO;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.attribute.expression.language.PreparedQuery;
import org.apache.nifi.attribute.expression.language.Query;
import org.apache.nifi.processors.standard.util.jolt.TransformFactory;
import org.apache.nifi.processors.standard.util.jolt.TransformUtils;
import org.apache.nifi.util.file.classloader.ClassLoaderUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.Map;

public class TransformJSONResource{

    private final static String DEFAULT_CHARSET = "UTF-8";

    protected Object getSpecificationJsonObject(JoltSpecificationDTO specificationDTO, boolean evaluateAttributes){

        if (!StringUtils.isEmpty(specificationDTO.getSpecification()) ){

            final String specification;

            if(evaluateAttributes) {
                PreparedQuery preparedQuery = Query.prepare(specificationDTO.getSpecification());
                Map<String, String> attributes = specificationDTO.getExpressionLanguageAttributes() == null ? Collections.emptyMap() : specificationDTO.getExpressionLanguageAttributes();
                specification = preparedQuery.evaluateExpressions(attributes, null);
            }else{
                specification = specificationDTO.getSpecification().replaceAll("\\$\\{","\\\\\\\\\\$\\{");
            }
            return JsonUtils.jsonToObject(specification, DEFAULT_CHARSET);

        }else{
            return null;
        }
    }

    /**
     * 校验 spec 格式
     * @author zhongwei.long
     * @date 2021/10/11 上午10:14
     * @param specificationDTO
     * @return com.lezw.transformjson.dto.ValidationDTO
     */
    public ValidationDTO validateSpec(JoltSpecificationDTO specificationDTO) {

        try {
            getTransformation(specificationDTO,false);
        }catch(final Exception e){
            //logger.error("Validation Failed - " + e.toString());
            return new ValidationDTO(false,"Validation Failed - Please verify the provided specification.");
        }

        return new ValidationDTO(true,"Specification is Valid!");
    }

    /**
      * 执行 JOLT 转换
      * @author zhongwei.long
      * @date 2021/10/11 上午10:15
      * @param specificationDTO
      * @return java.lang.String
      **/
    public String executeSpec(JoltSpecificationDTO specificationDTO) {

        try {
            JoltTransform transform = getTransformation(specificationDTO,true);
            Object inputJson = JsonUtils.jsonToObject(specificationDTO.getInput());
            return JsonUtils.toJsonString(TransformUtils.transform(transform,inputJson));

        }catch(final Exception e){
            //logger.error("Execute Specification Failed - " + e.toString());
            return "Execute Specification Failed";
        }

    }

    public JoltTransform getTransformation(JoltSpecificationDTO specificationDTO, boolean evaluateAttributes) throws Exception {

        Object specJson = getSpecificationJsonObject(specificationDTO,evaluateAttributes);
        String transformName = specificationDTO.getTransform();
        String modules = specificationDTO.getModules();

        ClassLoader classLoader = null;
        JoltTransform transform ;

        if(modules != null && !modules.isEmpty()){
            classLoader = ClassLoaderUtils.getCustomClassLoader(specificationDTO.getModules(),this.getClass().getClassLoader(), getJarFilenameFilter());
        } else{
            classLoader = this.getClass().getClassLoader();
        }

        if(transformName.equals("jolt-transform-custom")) {
            transform = TransformFactory.getCustomTransform(classLoader,specificationDTO.getCustomClass(), specJson);
        }else{
            transform = TransformFactory.getTransform(classLoader,specificationDTO.getTransform(), specJson);
        }

        return transform;
    }

    protected FilenameFilter getJarFilenameFilter(){
        return  new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return (name != null && name.endsWith(".jar"));
            }
        };
    }

}
