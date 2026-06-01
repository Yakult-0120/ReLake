import request, { type R } from './request'

export interface KafkaTopic {
  name: string
  partitions: number
  replicationFactor: number
  internal: boolean
}

export interface KafkaAcl {
  principal: string
  resourceType: string
  resourceName: string
  operation: string
  permissionType: string
  host: string
}

export interface AclCreateRequest {
  principal: string
  resourceType: string
  resourceName: string
  operation: string
  permissionType: string
  host: string
}

export interface ConsumerGroupSummary {
  groupId: string
  state: string
  members: number
  subscribedTopics: number
  activeTopics: number
  totalLag: number
}

export interface PartitionOffset {
  topic: string
  partition: number
  currentOffset: number
  endOffset: number
  lag: number
}

export interface MemberInfo {
  memberId: string
  clientId: string
  host: string
  partitions: PartitionOffset[]
}

export interface ConsumerGroupDetail {
  groupId: string
  members: MemberInfo[]
}

// ====== Topic ======
export function listTopics(targetId: number | string) {
  return request.get<any, R<KafkaTopic[]>>(`/api/v1/kafka/${targetId}/topics`)
}

export function createTopic(targetId: number | string, data: { topicName: string; numPartitions: number; replicationFactor: number }) {
  return request.post<any, R<void>>(`/api/v1/kafka/${targetId}/topics`, data)
}

export function deleteTopic(targetId: number | string, topicName: string) {
  return request.delete(`/api/v1/kafka/${targetId}/topics/${encodeURIComponent(topicName)}`)
}

// ====== ACL ======
export function listAcls(targetId: number | string) {
  return request.get<any, R<KafkaAcl[]>>(`/api/v1/kafka/${targetId}/acls`)
}

export function createAcl(targetId: number | string, data: AclCreateRequest) {
  return request.post<any, R<void>>(`/api/v1/kafka/${targetId}/acls`, data)
}

export function deleteAcl(targetId: number | string, data: AclCreateRequest) {
  return request.delete<any, R<void>>(`/api/v1/kafka/${targetId}/acls`, { data })
}

export function createScramUser(targetId: number | string, username: string, password: string) {
  return request.post<any, R<void>>(`/api/v1/kafka/${targetId}/scram-users`, { username, password })
}

// ====== Consumer Group ======
export function listConsumerGroups(targetId: number | string) {
  return request.get<any, R<ConsumerGroupSummary[]>>(`/api/v1/kafka/${targetId}/consumer-groups`)
}

export function describeConsumerGroup(targetId: number | string, groupId: string) {
  return request.get<any, R<ConsumerGroupDetail>>(`/api/v1/kafka/${targetId}/consumer-groups/${encodeURIComponent(groupId)}`)
}
