// 模块：图片处理 —— 头像上传前压缩到 512px，减少流量和后端存储。

const MAX_DIMENSION = 512
const COMPRESS_THRESHOLD = 512 * 1024

export async function compressAvatar(file: File): Promise<Blob> {
  if (file.size <= COMPRESS_THRESHOLD) return file
  try {
    const bitmap = await createImageBitmap(file)
    const scale = Math.min(1, MAX_DIMENSION / Math.max(bitmap.width, bitmap.height))
    const canvas = document.createElement('canvas')
    canvas.width = Math.max(1, Math.round(bitmap.width * scale))
    canvas.height = Math.max(1, Math.round(bitmap.height * scale))
    const context = canvas.getContext('2d')
    if (!context) {
      bitmap.close()
      return file
    }
    context.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
    bitmap.close()
    const blob = await new Promise<Blob | null>((resolve) =>
      canvas.toBlob(resolve, 'image/jpeg', 0.85),
    )
    return blob && blob.size < file.size ? blob : file
  } catch {
    // 浏览器不支持或图片损坏时退回原图，由后端做最终校验
    return file
  }
}
